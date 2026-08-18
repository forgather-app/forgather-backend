package com.forgather.domain.term.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.term.dto.TermAgreementResponse;
import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermAgreement;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TermService {

    private final TermRepository termRepository;
    private final HostTermHistoryRepository hostTermHistoryRepository;

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
}
