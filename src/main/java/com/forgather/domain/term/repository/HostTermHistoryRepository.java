package com.forgather.domain.term.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.TermType;

public interface HostTermHistoryRepository {

    <S extends HostTermHistory> Iterable<S> saveAll(Iterable<S> histories);

    HostTermHistory save(HostTermHistory history);

    @Query("""
        SELECT DISTINCT t.type
        FROM HostTermHistory h
        JOIN h.term t
        WHERE h.host.id = :hostId
          AND h.action = com.forgather.domain.term.model.HostTermHistoryAction.AGREE
          AND h.deletedAt IS NULL
        """)
    Set<TermType> findAgreedTermTypesByHostId(@Param("hostId") Long hostId);

    /**
     * 호스트의 약관 타입별 마지막 이력을 조회한다.
     * JOIN FETCH h.term은 판정에 필요한 "동의 당시 약관"(타입·버전)을 함께 로드해 1쿼리를 보장한다.
     */
    @Query("""
        SELECT h
        FROM HostTermHistory h
        JOIN FETCH h.term
        WHERE h.id IN (
            SELECT MAX(latest.id)
            FROM HostTermHistory latest
            WHERE latest.host = :host
              AND latest.deletedAt IS NULL
            GROUP BY latest.term.type
        )
        """)
    List<HostTermHistory> findLatestHistoriesPerTypeByHost(@Param("host") Host host);

    /**
     * 호스트의 특정 약관 타입에 대한 마지막 이력을 조회한다.
     * JOIN FETCH h.term은 판정에 필요한 "동의 당시 약관"(타입·버전)을 함께 로드해 1쿼리를 보장한다.
     */
    @Query("""
        SELECT h
        FROM HostTermHistory h
        JOIN FETCH h.term
        WHERE h.id = (
            SELECT MAX(latest.id)
            FROM HostTermHistory latest
            WHERE latest.host = :host
              AND latest.term.type = :type
              AND latest.deletedAt IS NULL
        )
        """)
    Optional<HostTermHistory> findLastHistoryByHostAndType(@Param("host") Host host, @Param("type") TermType type);
}
