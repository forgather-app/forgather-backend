package com.forgather.domain.term.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.TermRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TermService {

    private final TermRepository termRepository;

    public List<TermResponse> getLatestTerms() {
        List<Term> terms = termRepository.findLatestTerms();
        return terms.stream()
            .map(TermResponse::new)
            .toList();
    }
}
