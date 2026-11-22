package com.forgather.domain.export.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.export.model.SpacePdfExport;

public interface SpacePdfExportRepository {

    SpacePdfExport save(SpacePdfExport export);

    Optional<SpacePdfExport> findByJobId(String jobId);

    @Query("""
        SELECT COUNT(e) FROM SpacePdfExport e
        WHERE e.hostId = :hostId AND e.createdAt >= :start AND e.createdAt < :end
        """)
    long countByHostIdAndCreatedAtRange(@Param("hostId") Long hostId,
        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
