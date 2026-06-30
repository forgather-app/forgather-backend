package com.forgather.domain.term.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.forgather.domain.term.model.Term;

public interface TermRepository {

    @Query("""
        SELECT t
        FROM Term t
        WHERE t.id IN (
                SELECT MAX(latest.id)
                FROM Term latest
                WHERE latest.deletedAt IS NULL
                GROUP BY latest.type
            )
        ORDER BY t.sortOrder ASC
        """)
    List<Term> findLatestTerms();
}
