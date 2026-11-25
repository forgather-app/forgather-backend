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
        ReflectionTestUtils.setField(failure, "jobId", "b2574c32-10fb-4d65-b351-2a193a11941a");
        ReflectionTestUtils.setField(failure, "stage", stage);
        ReflectionTestUtils.setField(failure, "functionArn", "arn:aws:lambda:test");
        ReflectionTestUtils.setField(failure, "requestId", requestId);
        ReflectionTestUtils.setField(failure, "failureCondition", "RetriesExhausted");
        ReflectionTestUtils.setField(failure, "approximateInvokeCount", 3);
        ReflectionTestUtils.setField(failure, "errorType", "java.lang.IllegalStateException");
        ReflectionTestUtils.setField(failure, "errorMessage", "<script>alert('failure')</script>");
        ReflectionTestUtils.setField(failure, "rawEvent", "{\"detail\":\"failed\"}");
        ReflectionTestUtils.setField(failure, "processingStatus", processingStatus);
        ReflectionTestUtils.setField(failure, "processingNote", "이메일 인증 설정 확인 중");
        ReflectionTestUtils.setField(failure, "failedAt", failedAt);
        return failure;
    }
}
