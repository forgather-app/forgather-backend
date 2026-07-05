package com.forgather.global.auth.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.KakaoLoginTokenResponse;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtParser jwtParser;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoHostRepository kakaoHostRepository;
    private final HostRepository hostRepository;
    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;

    public KakaoLoginTokenResponse getKakaoLoginToken() {
        return new KakaoLoginTokenResponse(kakaoAuthClient.getKakaoClientId());
    }

    @Transactional
    public LoginResponse kakaoLoginConfirm(KakaoLoginConfirmRequest request) {
        KakaoHost kakaoHost = toKakaoHost(request);
        String accessToken = jwtTokenProvider.generateAccessToken(kakaoHost.getHost().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(kakaoHost.getHost().getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    private KakaoHost toKakaoHost(KakaoLoginConfirmRequest request) {
        KakaoIdToken idToken = jwtParser.parseIdToken(request.idToken());
        Optional<KakaoHost> kakaoHost = kakaoHostRepository.findByUserId(idToken.sub());
        return kakaoHost.orElseGet(() -> {
            Host host = new Host(idToken.nickname(), idToken.picture());
            KakaoHost newKakaoHost = new KakaoHost(host, idToken.sub());
            return kakaoHostRepository.save(newKakaoHost);
        });
    }

    public LoginResponse refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        Long hostId = jwtTokenProvider.getId(refreshToken);
        Host host = hostRepository.getByIdOrThrow(hostId);
        String accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    public HostResponse getCurrentUser(Host host) {
        return HostResponse.from(host, isOnboardingCompleted(host));
    }

    @Transactional
    public HostResponse submitOnboarding(Host loginHost, OnboardingRequest request) {
        List<Term> submittedTerms = getActiveTermsByIds(request.agreedTermIds());
        validateRequiredTermTypes(submittedTerms);

        Host host = hostRepository.getByIdOrThrow(loginHost.getId());
        host.updateNickname(request.nickname());

        List<HostTermHistory> hostTermHistories = submittedTerms.stream()
            .map(term -> new HostTermHistory(host, term, AGREE))
            .toList();
        hostTermHistoryRepository.saveAll(hostTermHistories);

        return HostResponse.from(host, true);
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
        List<Term> terms = termRepository.findByIdInAndDeletedAtIsNull(termIds);
        if (terms.size() != termIds.size()) {
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
