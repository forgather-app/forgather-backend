package com.forgather.domain.term.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.TermType;

public interface HostTermHistoryRepository {

    <S extends HostTermHistory> Iterable<S> saveAll(Iterable<S> histories);

    @Query("""
        SELECT DISTINCT t.type
        FROM HostTermHistory h
        JOIN h.term t
        WHERE h.host.id = :hostId
          AND h.action = com.forgather.domain.term.model.HostTermHistoryAction.AGREE
          AND h.deletedAt IS NULL
        """)
    Set<TermType> findAgreedTermTypesByHostId(@Param("hostId") Long hostId);
}
