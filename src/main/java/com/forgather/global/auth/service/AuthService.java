package com.forgather.global.auth.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.client.AppleAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.AppleTokenResponse;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtParser jwtParser;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoHostRepository kakaoHostRepository;
    private final HostRepository hostRepository;
    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;
    private final AppleHostRepository appleHostRepository;
    private final AppleAuthClient appleAuthClient;
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
        KakaoIdToken idToken = jwtParser.parseKakaoIdToken(request.idToken(), request.rawNonce());
        Optional<KakaoHost> kakaoHost = kakaoHostRepository.findByUserId(idToken.sub());
        if (kakaoHost.isPresent()) {
            KakaoHost existingKakaoHost = kakaoHost.get();
            existingKakaoHost.getHost().updateEmail(idToken.email());
            return existingKakaoHost;
        }

        Host host = hostRepository.save(
            new Host(codeGenerator.generate(Host.CODE_LENGTH), idToken.nickname(), idToken.email()));
        KakaoHost newKakaoHost = new KakaoHost(host, idToken.sub());
        return kakaoHostRepository.save(newKakaoHost);
    }

    @Transactional
    public LoginResponse appleLoginConfirm(AppleLoginConfirmRequest request) {
        AppleTokenResponse appleToken = appleAuthClient.exchangeAuthorizationCode(request.authorizationCode());
        AppleIdToken idToken = jwtParser.parseAppleIdToken(appleToken.idToken(), request.rawNonce());
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

        Host host = new Host(codeGenerator.generate(Host.CODE_LENGTH), fullName, idToken.email());
        hostRepository.save(host);
        return appleHostRepository.save(new AppleHost(host, idToken.sub(), appleRefreshToken));
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
        return HostResponse.of(host, findProfilePhoto(host), isOnboardingCompleted(host));
    }

    private HostProfilePhoto findProfilePhoto(Host host) {
        return hostProfilePhotoRepository.findByHostAndDeletedAtIsNull(host).orElse(null);
    }

    @Transactional
    public HostResponse submitOnboarding(Host loginHost, OnboardingRequest request) {
        if (request.agreedTermIds() == null) {
            throw new BaseNullPointerException("동의 약관 목록은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<Term> submittedTerms = getActiveTermsByIds(request.agreedTermIds());
        validateRequiredTermTypes(submittedTerms);

        Host host = hostRepository.getByIdOrThrow(loginHost.getId());
        host.updateNickname(request.nickname());

        List<HostTermHistory> hostTermHistories = submittedTerms.stream()
            .map(term -> new HostTermHistory(host, term, AGREE))
            .toList();
        hostTermHistoryRepository.saveAll(hostTermHistories);

        return HostResponse.of(host, findProfilePhoto(host), true);
    }

    private boolean isOnboardingCompleted(Host host) {
        if (!host.hasValidNickname()) {
            return false;
        }
        Set<TermType> agreedTypes = hostTermHistoryRepository.findAgreedTermTypesByHostId(host.getId());
        return agreedTypes.containsAll(TermType.requiredTypes());
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

    private void validateRequiredTermTypes(List<Term> submittedTerms) {
        Set<TermType> requiredTypes = TermType.requiredTypes();
        Set<TermType> submittedTypes = submittedTerms.stream()
            .map(Term::getType)
            .collect(Collectors.toSet());
        if (!submittedTypes.containsAll(requiredTypes)) {
            throw new BaseException("필수 약관 동의가 누락되었습니다. requiredTypes: " + requiredTypes);
        }
    }

}
