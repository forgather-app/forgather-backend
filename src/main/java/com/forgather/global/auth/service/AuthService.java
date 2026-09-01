package com.forgather.global.auth.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.external.social.AppleApiClient;
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
    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;
    private final AppleHostRepository appleHostRepository;
    private final AppleApiClient appleApiClient;
    private final HostProfilePhotoRepository hostProfilePhotoRepository;
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

    public HostResponse getCurrentUser(Host host) {
        Set<TermType> agreedTypes = hostTermHistoryRepository.findAgreedTermTypesByHostId(host.getId());
        return HostResponse.of(host, findProfilePhoto(host), host.isOnboardingCompleted(agreedTypes));
    }

    private HostProfilePhoto findProfilePhoto(Host host) {
        return hostProfilePhotoRepository.findByHostAndDeletedAtIsNull(host).orElse(null);
    }

    /**
     * 완료 검사와 이력 저장 사이에 동시 요청이 끼어들면 두 요청 모두 미완료로 읽고 각각 이력을 남길 수 있다.
     * 호스트 행에 배타 락을 걸어 동시 온보딩을 직렬화한다.
     */
    @Transactional
    public HostResponse submitOnboarding(Host loginHost, OnboardingRequest request) {
        if (request.agreedTermIds() == null) {
            throw new BaseNullPointerException("동의 약관 목록은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        Host host = hostRepository.getByIdWithLockOrThrow(loginHost.getId());
        validateOnboardingNotCompleted(host);

        List<Term> agreedTerms = getActiveTermsByIds(request.agreedTermIds());
        List<Term> rejectedTerms = getActiveTermsByIds(request.rejectedTermIds());
        validateRejectedTermsAreOptional(rejectedTerms);
        validateRequiredTermTypes(agreedTerms);
        validateNoDuplicatedTypeDecision(agreedTerms, rejectedTerms);
        validateAllLatestTermsDecided(agreedTerms, rejectedTerms);

        host.updateNickname(request.nickname());

        List<HostTermHistory> hostTermHistories = Stream.concat(
                agreedTerms.stream().map(term -> new HostTermHistory(host, term, AGREE)),
                rejectedTerms.stream().map(term -> new HostTermHistory(host, term, REJECT)))
            .toList();
        hostTermHistoryRepository.saveAll(hostTermHistories);

        return HostResponse.of(host, findProfilePhoto(host), true);
    }

    /**
     * 온보딩 재호출이 REJECT 이력을 append해 기존 동의를 조용히 뒤집는 것을 막는다.
     * 동의/철회 상태 변경은 별도 토글 API의 책임이다.
     */
    private void validateOnboardingNotCompleted(Host host) {
        Set<TermType> agreedTypes = hostTermHistoryRepository.findAgreedTermTypesByHostId(host.getId());
        if (host.isOnboardingCompleted(agreedTypes)) {
            throw new ConflictException("이미 온보딩이 완료된 호스트입니다. hostId: " + host.getId());
        }
    }

    private void validateRejectedTermsAreOptional(List<Term> rejectedTerms) {
        Set<TermType> rejectedRequiredTypes = rejectedTerms.stream()
            .map(Term::getType)
            .filter(TermType::isRequired)
            .collect(Collectors.toSet());
        if (!rejectedRequiredTypes.isEmpty()) {
            throw new BaseException("필수 약관은 거절할 수 없습니다. rejectedRequiredTypes: " + rejectedRequiredTypes);
        }
    }

    private void validateNoDuplicatedTypeDecision(List<Term> agreedTerms, List<Term> rejectedTerms) {
        Set<TermType> agreedTypes = toTermTypes(agreedTerms);
        Set<TermType> duplicatedTypes = toTermTypes(rejectedTerms).stream()
            .filter(agreedTypes::contains)
            .collect(Collectors.toSet());
        if (!duplicatedTypes.isEmpty()) {
            throw new BaseException(
                "같은 타입의 약관을 동의와 거절에 함께 보낼 수 없습니다. duplicatedTypes: " + duplicatedTypes);
        }
    }

    /**
     * 타입별 최신 약관 전체에 대해 동의 또는 거절 결정이 명시되어야 한다.
     */
    private void validateAllLatestTermsDecided(List<Term> agreedTerms, List<Term> rejectedTerms) {
        Set<Long> latestTermIds = termRepository.findLatestTerms().stream()
            .map(Term::getId)
            .collect(Collectors.toSet());
        Set<Long> decidedTermIds = Stream.concat(agreedTerms.stream(), rejectedTerms.stream())
            .map(Term::getId)
            .collect(Collectors.toSet());
        if (!decidedTermIds.equals(latestTermIds)) {
            throw new BaseException(
                "타입별 최신 약관 전체에 대한 동의 또는 거절이 필요합니다. latestTermIds: %s, decidedTermIds: %s"
                    .formatted(latestTermIds, decidedTermIds));
        }
    }

    private Set<TermType> toTermTypes(List<Term> terms) {
        return terms.stream()
            .map(Term::getType)
            .collect(Collectors.toSet());
    }

    private List<Term> getActiveTermsByIds(List<Long> termIds) {
        if (termIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctTermIds = termIds.stream().distinct().toList();
        List<Term> terms = termRepository.findByIdInAndDeletedAtIsNull(distinctTermIds);
        if (terms.size() != distinctTermIds.size()) {
            throw new BaseException("존재하지 않거나 삭제된 약관 ID가 포함되어 있습니다. termIds: " + termIds);
        }

        return terms;
    }

    private void validateRequiredTermTypes(List<Term> agreedTerms) {
        Set<TermType> requiredTypes = TermType.requiredTypes();
        Set<TermType> submittedTypes = agreedTerms.stream()
            .map(Term::getType)
            .collect(Collectors.toSet());
        if (!submittedTypes.containsAll(requiredTypes)) {
            throw new BaseException("필수 약관 동의가 누락되었습니다. requiredTypes: " + requiredTypes);
        }
    }

}
