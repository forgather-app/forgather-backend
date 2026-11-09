package com.forgather.back_office.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.back_office.repository.AdminPdfExportFailureRepository;
import com.forgather.domain.export.model.SpacePdfExportFailure;

public interface AdminPdfExportFailureJpaRepository
    extends JpaRepository<SpacePdfExportFailure, Long>, AdminPdfExportFailureRepository {
}
