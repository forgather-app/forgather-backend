package com.forgather.global.auth.dev;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.global.auth.service.AuthService;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile({"local", "dev", "test"})
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevAuthService {

    private final DevLoginProperties devLoginProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoHostRepository kakaoHostRepository;
    private final HostRepository hostRepository;
    private final TermRepository termRepository;
    private final AuthService authService;
    private final RandomCodeGenerator codeGenerator;

    @Transactional
    public LoginResponse login(DevLoginRequest request) {
        validateCredentials(request);

        String userId = devLoginProperties.getLoginId();
        KakaoHost kakaoHost = kakaoHostRepository.findByUserId(userId)
            .orElseGet(() -> createOnboardedHost(userId));
        Long hostId = kakaoHost.getHost().getId();

        log.warn("개발용 임시 로그인이 사용되었습니다. hostId: {}", hostId);
        return LoginResponse.of(
            jwtTokenProvider.generateAccessToken(hostId),
            jwtTokenProvider.generateRefreshToken(hostId)
        );
    }

    private void validateCredentials(DevLoginRequest request) {
        boolean loginIdMatches = matches(request.loginId(), devLoginProperties.getLoginId());
        boolean passwordMatches = matches(request.password(), devLoginProperties.getPassword());
        if (!loginIdMatches || !passwordMatches) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    private boolean matches(String input, String expected) {
        if (input == null) {
            return false;
        }
        return MessageDigest.isEqual(
            input.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private KakaoHost createOnboardedHost(String userId) {
        Host host = hostRepository.save(new Host(
            codeGenerator.generate(Host.CODE_LENGTH),
            devLoginProperties.getNickname(),
            devLoginProperties.getEmail()));
        KakaoHost savedKakaoHost = kakaoHostRepository.save(new KakaoHost(host, userId));
        completeOnboarding(savedKakaoHost.getHost());
        return savedKakaoHost;
    }

    /**
     * 실제 온보딩과 상태가 갈라지지 않도록 AuthService.submitOnboarding을 그대로 재사용한다.
     * 최초 생성 시에만 호출되므로 약관 동의 이력이 중복 저장되지 않는다.
     * 온보딩은 모든 약관 타입에 대한 결정을 요구하므로 선택 약관은 거절로 명시한다.
     */
    private void completeOnboarding(Host host) {
        Map<Boolean, List<Long>> termIdsByRequired = termRepository.findLatestTerms()
            .stream()
            .collect(Collectors.partitioningBy(
                Term::isRequiredType,
                Collectors.mapping(Term::getId, Collectors.toList())));
        authService.submitOnboarding(host, new OnboardingRequest(
            devLoginProperties.getNickname(),
            termIdsByRequired.get(true),
            termIdsByRequired.get(false)));
    }
}
