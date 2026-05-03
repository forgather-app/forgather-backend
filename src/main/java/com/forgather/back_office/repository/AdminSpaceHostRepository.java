package com.forgather.back_office.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;

public interface AdminSpaceHostRepository {

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.name, h.createdAt,
                (SELECT COUNT(sh.id)
                 FROM SpaceHost sh JOIN sh.space s
                 WHERE sh.host = h AND s.deletedAt IS NULL
                )
            )
            FROM Host h
            """,
        countQuery = "SELECT COUNT(h) FROM Host h"
    )
    Page<HostDetailResponse> findAllHostsWithSpaceCount(Pageable pageable);

    @Query("""
        SELECT sh
        FROM SpaceHost sh
            JOIN FETCH sh.space s
        WHERE sh.host = :host
            AND s.deletedAt IS NULL
            AND sh.deletedAt IS NULL
        ORDER BY s.createdAt DESC
        """)
    List<SpaceHost> findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(@Param("host") Host host);
}
