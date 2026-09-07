package com.forgather.domain.auth.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.forgather.domain.auth.dto.AppleLoginConfirmRequest;
import com.forgather.domain.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.domain.auth.dto.LoginResponse;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.SocialJwtParser;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.dto.AppleIdToken;
import com.forgather.global.external.social.dto.AppleTokenResponse;
import com.forgather.global.external.social.dto.KakaoIdToken;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final int MAX_HOST_CODE_ATTEMPTS = 5;
    private static final String DEFAULT_APPLE_HOST_NAME = "Apple 사용자";

    private final SocialJwtParser socialJwtParser;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoHostRepository kakaoHostRepository;
    private final HostRepository hostRepository;
    private final AppleHostRepository appleHostRepository;
    private final AppleApiClient appleApiClient;
    private final RandomCodeGenerator codeGenerator;

    @Transactional
    public LoginResponse kakaoLoginConfirm(KakaoLoginConfirmRequest request) {
        KakaoHost kakaoHost = toKakaoHost(request);
        String accessToken = jwtTokenProvider.generateAccessToken(kakaoHost.getHost().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(kakaoHost.getHost().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    /**
     * 기존 회원은 재로그인할 때마다 id token의 이메일로 갱신한다.
     * 이메일 저장 이전에 가입한 회원의 빈 이메일을 별도 배치 없이 점진적으로 채우기 위함이다.
     */
    private KakaoHost toKakaoHost(KakaoLoginConfirmRequest request) {
        KakaoIdToken idToken = socialJwtParser.parseKakaoIdToken(request.idToken(), request.rawNonce());
        Optional<KakaoHost> kakaoHost = kakaoHostRepository.findByUserId(idToken.sub());
        if (kakaoHost.isPresent()) {
            KakaoHost existingKakaoHost = kakaoHost.get();
            existingKakaoHost.getHost().updateEmail(idToken.email());
            return existingKakaoHost;
        }

        Host host = hostRepository.save(
            new Host(generateUnusedHostCode(), idToken.nickname(), idToken.email()));
        logNewHost(SocialProvider.KAKAO, host);
        KakaoHost newKakaoHost = new KakaoHost(host, idToken.sub());
        return kakaoHostRepository.save(newKakaoHost);
    }

    @Transactional
    public LoginResponse appleLoginConfirm(AppleLoginConfirmRequest request) {
        AppleTokenResponse appleToken = appleApiClient.exchangeAuthorizationCode(request.authorizationCode());
        AppleIdToken idToken = socialJwtParser.parseAppleIdToken(appleToken.idToken(), request.rawNonce());
        AppleHost appleHost = toAppleHost(request.fullName(), idToken, appleToken.refreshToken());
        String accessToken = jwtTokenProvider.generateAccessToken(appleHost.getHost().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(appleHost.getHost().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    private AppleHost toAppleHost(
        String fullName,
        AppleIdToken idToken,
        String appleRefreshToken
    ) {
        Optional<AppleHost> appleHost = appleHostRepository.findByUserId(idToken.sub());
        if (appleHost.isPresent()) {
            AppleHost existingAppleHost = appleHost.get();
            existingAppleHost.updateRefreshToken(appleRefreshToken);
            return existingAppleHost;
        }

        Host host = new Host(generateUnusedHostCode(), resolveAppleName(fullName), idToken.email());
        hostRepository.save(host);
        logNewHost(SocialProvider.APPLE, host);
        return appleHostRepository.save(new AppleHost(host, idToken.sub(), appleRefreshToken));
    }

    /**
     * Apple은 최초 동의 시점에만 이름을 내려준다. 최초 로그인이 Host 저장 전에 실패했거나
     * 연결 해제 없이 탈퇴한 뒤 재가입하면 Apple 매핑만 남아 이후 로그인에서 이름을 받을 수 없다.
     * 가입을 막으면 사용자가 직접 Apple 연결을 해제하기 전까지 영구히 로그인할 수 없으므로
     * 기본 이름으로 대체한다. 사용자에게 보이는 이름은 온보딩에서 받는 nickname이라 영향이 없다.
     */
    private String resolveAppleName(String fullName) {
        if (StringUtils.hasText(fullName)) {
            return fullName;
        }
        log.warn("Apple 신규 가입에 이름이 없어 기본 이름으로 대체합니다.");
        return DEFAULT_APPLE_HOST_NAME;
    }

    private void logNewHost(SocialProvider provider, Host host) {
        log.info("신규 회원 가입 완료. provider: {}, hostCode: {}", provider, host.getCode());
    }

    private String generateUnusedHostCode() {
        for (int attempt = 0; attempt < MAX_HOST_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate(Host.CODE_LENGTH);
            if (!hostRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BaseException(
            "호스트 코드를 생성하지 못했습니다. 시도 횟수: %d".formatted(MAX_HOST_CODE_ATTEMPTS),
            HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public LoginResponse refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        Long hostId = jwtTokenProvider.getId(refreshToken);
        Host host = hostRepository.findByIdAndDeletedAtIsNull(hostId)
            .orElseThrow(() -> new UnauthorizedException("탈퇴했거나 존재하지 않는 호스트입니다. id: " + hostId));
        String accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        return LoginResponse.of(accessToken, refreshToken);
    }
}
