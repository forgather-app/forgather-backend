package com.forgather.domain.export.repository;

import java.util.Optional;

import com.forgather.domain.export.model.SpacePdfExport;

public interface SpacePdfExportRepository {

    SpacePdfExport save(SpacePdfExport export);

    Optional<SpacePdfExport> findByJobId(String jobId);
}
