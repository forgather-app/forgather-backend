package com.forgather.global.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.AppUserResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.KakaoLoginTokenResponse;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.KakaoAppUser;
import com.forgather.global.auth.repository.KakaoAppUserRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtParser jwtParser;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoAppUserRepository kakaoAppUserRepository;
    private final AppUserRepository appUserRepository;

    public KakaoLoginTokenResponse getKakaoLoginToken() {
        return new KakaoLoginTokenResponse(kakaoAuthClient.getKakaoClientId());
    }

    @Transactional
    public LoginResponse kakaoLoginConfirm(KakaoLoginConfirmRequest request) {
        KakaoAppUser kakaoAppUser = toKakaoAppUser(request);
        String accessToken = jwtTokenProvider.generateAccessToken(kakaoAppUser.getAppUser().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(kakaoAppUser.getAppUser().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    private KakaoAppUser toKakaoAppUser(KakaoLoginConfirmRequest request) {
        KakaoIdToken idToken = jwtParser.parseIdToken(request.idToken());
        Optional<KakaoAppUser> kakaoAppUser = kakaoAppUserRepository.findByUserId(idToken.sub());
        AppUser appUser = new AppUser(idToken.nickname(), idToken.picture());

        return kakaoAppUser.orElseGet(() -> kakaoAppUserRepository.save(new KakaoAppUser(appUser, idToken.sub())));
    }

    public LoginResponse refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        Long appUserId = jwtTokenProvider.getId(refreshToken);
        AppUser appUser = appUserRepository.getByIdOrThrow(appUserId);
        String accessToken = jwtTokenProvider.generateAccessToken(appUser.getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    public AppUserResponse getCurrentUser(AppUser appUser) {
        return AppUserResponse.from(appUser);
    }

    @Transactional
    public void agreeTerms(AppUser appUser) {
        appUser.agreeTerms();
    }
}
