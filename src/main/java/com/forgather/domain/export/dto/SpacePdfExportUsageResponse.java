package com.forgather.domain.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpacePdfExportUsageResponse(
    @Schema(description = "오늘 접수된 PDF 추출 요청 수", example = "2")
    long count,
    @Schema(description = "하루 PDF 추출 요청 한도", example = "5")
    long limit
) {
}
