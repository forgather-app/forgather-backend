package com.forgather.domain.term.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.term.dto.TermAgreementResponse;
import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermAgreement;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TermService {

    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;
    private final HostRepository hostRepository;

    public List<TermResponse> getLatestTerms() {
        List<Term> terms = termRepository.findLatestTerms();
        return terms.stream()
            .map(TermResponse::from)
            .toList();
    }

    public List<TermAgreementResponse> getMyTermAgreements(Host host) {
        List<Term> latestTerms = termRepository.findLatestTerms();
        Map<TermType, HostTermHistory> lastHistoryByType =
            hostTermHistoryRepository.findLatestHistoriesPerTypeByHost(host).stream()
                .collect(Collectors.toMap(
                    history -> history.getTerm().getType(),
                    Function.identity()));
        return latestTerms.stream()
            .map(term -> new TermAgreement(term, lastHistoryByType.get(term.getType())))
            .map(TermAgreementResponse::from)
            .toList();
    }

    @Transactional
    public TermAgreementResponse agreeTerm(Host loginHost, Long termId) {
        // 락을 첫 조회로 둬야 스냅샷이 락 획득 이후에 확정된다.
        // 앞에 일반 조회가 오면 그 시점 스냅샷이 고정돼, 락을 기다리는 동안 커밋된 이력을 보지 못한다.
        Host host = hostRepository.getByIdWithLockOrThrow(loginHost.getId());

        Term term = termRepository.getByIdAndDeletedAtIsNullOrThrow(termId);
        validateLatestTerm(term);
        validateOnboardingCompleted(host);

        TermAgreement agreement = getTermAgreement(host, term);
        if (agreement.isAgreed()) {
            return TermAgreementResponse.from(agreement);
        }

        HostTermHistory history = hostTermHistoryRepository.save(new HostTermHistory(host, term, AGREE));
        return TermAgreementResponse.from(new TermAgreement(term, history));
    }

    @Transactional
    public TermAgreementResponse withdrawTerm(Host loginHost, Long termId) {
        Host host = hostRepository.getByIdWithLockOrThrow(loginHost.getId());

        Term term = termRepository.getByIdAndDeletedAtIsNullOrThrow(termId);
        validateLatestTerm(term);
        validateWithdrawable(term);
        validateOnboardingCompleted(host);

        TermAgreement agreement = getTermAgreement(host, term);
        if (!agreement.isLastActionAgree()) {
            return TermAgreementResponse.from(agreement);
        }

        HostTermHistory history = hostTermHistoryRepository.save(new HostTermHistory(host, term, WITHDRAW));
        return TermAgreementResponse.from(new TermAgreement(term, history));
    }

    private void validateOnboardingCompleted(Host host) {
        Set<TermType> agreedTypes = hostTermHistoryRepository.findAgreedTermTypesByHostId(host.getId());
        if (!host.isOnboardingCompleted(agreedTypes)) {
            throw new BaseException("온보딩이 완료되지 않은 호스트는 약관 동의 상태를 변경할 수 없습니다. hostId: %d"
                .formatted(host.getId()));
        }
    }

    private void validateWithdrawable(Term term) {
        if (term.isRequiredType()) {
            throw new BaseException("필수 약관은 철회할 수 없습니다. termId: %d, type: %s"
                .formatted(term.getId(), term.getType()));
        }
    }

    private void validateLatestTerm(Term term) {
        Term latestTerm = termRepository.getLatestTermByTypeOrThrow(term.getType());
        if (!latestTerm.getId().equals(term.getId())) {
            throw new BaseException("최신 약관이 아닙니다. termId: %d, latestTermId: %d"
                .formatted(term.getId(), latestTerm.getId()));
        }
    }

    private TermAgreement getTermAgreement(Host host, Term latestTerm) {
        HostTermHistory lastHistory = hostTermHistoryRepository
            .findLastHistoryByHostAndType(host, latestTerm.getType())
            .orElse(null);
        return new TermAgreement(latestTerm, lastHistory);
    }
}
