package com.forgather.back_office.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;

public interface AdminSpaceHostMapRepository {

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
