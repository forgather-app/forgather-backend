package com.forgather.domain.host.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.dto.HostResponse;
import com.forgather.domain.host.dto.OnboardingRequest;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostAccountService {

    private final HostRepository hostRepository;
    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;
    private final HostProfilePhotoRepository hostProfilePhotoRepository;

    public HostResponse getAccount(Host host) {
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
