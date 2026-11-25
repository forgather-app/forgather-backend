package com.forgather.back_office.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;

import com.forgather.back_office.dto.AdminPdfExportFailureSummaryResponse;
import com.forgather.domain.export.model.SpacePdfExportFailure;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface AdminPdfExportFailureRepository {

    @Query("""
        SELECT new com.forgather.back_office.dto.AdminPdfExportFailureSummaryResponse(
            f.id, f.jobId, f.requestId, f.stage, f.errorType, f.processingStatus, f.failedAt
        )
        FROM SpacePdfExportFailure f
        WHERE f.deletedAt IS NULL
        """)
    Page<AdminPdfExportFailureSummaryResponse> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<SpacePdfExportFailure> findByIdAndDeletedAtIsNull(Long id);

    default SpacePdfExportFailure getByIdAndDeletedAtIsNullOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("PDF 추출 실패 id는 null일 수 없습니다. id: " + id, HttpStatus.BAD_REQUEST);
        }
        return findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 PDF 추출 실패입니다. id: " + id));
    }
}
