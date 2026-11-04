package com.forgather.domain.export.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.export.model.SpacePdfExport;
import com.forgather.domain.export.repository.SpacePdfExportRepository;

public interface SpacePdfExportJpaRepository extends JpaRepository<SpacePdfExport, Long>, SpacePdfExportRepository {
}
