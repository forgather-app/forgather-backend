package com.forgather.back_office.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;

public interface AdminSpaceHostMapRepository {

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.name, h.createdAt,
                (SELECT COUNT(shm.id)
                 FROM SpaceHostMap shm JOIN shm.space s
                 WHERE shm.host = h AND s.deletedAt IS NULL
                )
            )
            FROM Host h
            """,
        countQuery = "SELECT COUNT(h) FROM Host h"
    )
    Page<HostDetailResponse> findAllHostsWithSpaceCount(Pageable pageable);

    @Query("""
        SELECT shm
        FROM SpaceHostMap shm
            JOIN FETCH shm.space s
        WHERE shm.host = :host
            AND s.deletedAt IS NULL
            AND shm.deletedAt IS NULL
        ORDER BY s.createdAt DESC
        """)
    List<SpaceHostMap> findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(@Param("host") Host host);
}
