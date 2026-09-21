package com.forgather.domain.term.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.TermRepository;

public interface TermJpaRepository extends JpaRepository<Term, Long>, TermRepository {
}
