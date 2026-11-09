package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.export.model.PdfExportFailureProcessingStatus;
import com.forgather.domain.export.model.PdfExportFailureStage;
import com.forgather.domain.export.model.SpacePdfExportFailure;

public record AdminPdfExportFailureDetailResponse(
    Long id,
    String jobId,
    PdfExportFailureStage stage,
    String functionArn,
    String requestId,
    String failureCondition,
    Integer approximateInvokeCount,
    String errorType,
    String errorMessage,
    String rawEvent,
    PdfExportFailureProcessingStatus processingStatus,
    String processingNote,
    LocalDateTime failedAt,
    LocalDateTime createdAt
) {

    public static AdminPdfExportFailureDetailResponse from(SpacePdfExportFailure failure) {
        return new AdminPdfExportFailureDetailResponse(
            failure.getId(), failure.getJobId(), failure.getStage(), failure.getFunctionArn(),
            failure.getRequestId(), failure.getFailureCondition(), failure.getApproximateInvokeCount(),
            failure.getErrorType(), failure.getErrorMessage(), failure.getRawEvent(),
            failure.getProcessingStatus(), failure.getProcessingNote(), failure.getFailedAt(), failure.getCreatedAt()
        );
    }
}
