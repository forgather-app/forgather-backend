package com.forgather.global.auth.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.AppleUser;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.KakaoLoginTokenResponse;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtParser jwtParser;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoHostRepository kakaoHostRepository;
    private final HostRepository hostRepository;
    private final AppleHostRepository appleHostRepository;
    private final ObjectMapper objectMapper;

    public KakaoLoginTokenResponse getKakaoLoginToken() {
        return new KakaoLoginTokenResponse(kakaoAuthClient.getKakaoClientId());
    }

    @Transactional
    public LoginResponse kakaoLoginConfirm(KakaoLoginConfirmRequest request) {
        KakaoHost kakaoHost = toKakaoHost(request);
        String accessToken = jwtTokenProvider.generateAccessToken(kakaoHost.getHost().getId());
        String refreshToken= jwtTokenProvider.generateRefreshToken(kakaoHost.getHost().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    private KakaoHost toKakaoHost(KakaoLoginConfirmRequest request) {
        KakaoIdToken idToken = jwtParser.parseKakaoIdToken(request.idToken());
        Optional<KakaoHost> kakaoHost = kakaoHostRepository.findByUserId(idToken.sub());
        Host host = new Host(idToken.nickname(), idToken.picture());

        return kakaoHost.orElseGet(() -> kakaoHostRepository.save(new KakaoHost(host, idToken.sub())));
    }

    @Transactional
    public LoginResponse appleLoginConfirm(AppleLoginConfirmRequest request) {
        AppleHost appleHost = toAppleHost(request);
        String accessToken = jwtTokenProvider.generateAccessToken(appleHost.getHost().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(appleHost.getHost().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    private AppleHost toAppleHost(AppleLoginConfirmRequest request) {
        AppleIdToken idToken = jwtParser.parseAppleIdToken(request.idToken(), request.rawNonce());
        Optional<AppleHost> appleHost = appleHostRepository.findByUserId(idToken.sub());
        return appleHost.orElseGet(() -> {
            AppleUser appleUser = parseAppleUser(request.user());
            Host host = new Host(appleUser.fullName(), null, idToken.email());
            return appleHostRepository.save(new AppleHost(host, idToken.sub()));
        });
    }

    private AppleUser parseAppleUser(String userJson) {
        if (!StringUtils.hasText(userJson)) {
            throw new BaseException("회원가입을 위해 애플 사용자 정보가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        try {
            return objectMapper.readValue(userJson, AppleUser.class);
        } catch (JsonProcessingException e) {
            throw new BaseException("애플 사용자 정보 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST, e);
        }
    }

    public LoginResponse refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        Long hostId = jwtTokenProvider.getId(refreshToken);
        Host host = hostRepository.getByIdOrThrow(hostId);
        String accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    public HostResponse getCurrentUser(Host host) {
        return HostResponse.from(host);
    }

    @Transactional
    public void agreeTerms(Host host) {
        host.agreeTerms();
    }
}
