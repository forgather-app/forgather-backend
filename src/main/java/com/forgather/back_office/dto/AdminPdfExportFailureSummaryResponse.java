package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.export.model.PdfExportFailureProcessingStatus;
import com.forgather.domain.export.model.PdfExportFailureStage;

public record AdminPdfExportFailureSummaryResponse(
    Long id,
    String jobId,
    String requestId,
    PdfExportFailureStage stage,
    String errorType,
    PdfExportFailureProcessingStatus processingStatus,
    LocalDateTime failedAt
) {
}
