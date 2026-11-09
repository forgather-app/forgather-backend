package com.forgather.fixture;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.export.model.PdfExportFailureProcessingStatus;
import com.forgather.domain.export.model.PdfExportFailureStage;
import com.forgather.domain.export.model.SpacePdfExportFailure;

public class SpacePdfExportFailureFixture {

    private SpacePdfExportFailureFixture() {
    }

    public static SpacePdfExportFailure createFailure(
        String requestId,
        LocalDateTime failedAt,
        PdfExportFailureStage stage,
        PdfExportFailureProcessingStatus processingStatus
    ) {
        SpacePdfExportFailure failure = BeanUtils.instantiateClass(SpacePdfExportFailure.class);
        ReflectionTestUtils.setField(failure, "stage", stage);
        ReflectionTestUtils.setField(failure, "functionArn", "arn:aws:lambda:test");
        ReflectionTestUtils.setField(failure, "requestId", requestId);
        ReflectionTestUtils.setField(failure, "failureCondition", "RetriesExhausted");
        ReflectionTestUtils.setField(failure, "errorMessage", "<script>alert('failure')</script>");
        ReflectionTestUtils.setField(failure, "rawEvent", "{\"detail\":\"failed\"}");
        ReflectionTestUtils.setField(failure, "processingStatus", processingStatus);
        ReflectionTestUtils.setField(failure, "failedAt", failedAt);
        return failure;
    }
}
