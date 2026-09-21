package com.forgather.domain.guestbook.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.forgather.domain.guestbook.model.GuestBookReport;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReportHistoryDto(
    @Schema(example = "1")
    long id,

    @Schema(description = "신고 당시 방명록 작성 닉네임", example = "현주징징이")
    String nicknameSnapshot,

    @Schema(description = "신고 당시 방명록 내용", example = "역겹다;;;;")
    String messageSnapshot,

    @Schema(description = "신고 접수 시각", example = "2025-10-13T13:05")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime createdAt
) {

    public ReportHistoryDto(GuestBookReport report) {
        this(report.getId(), report.getNicknameSnapshot(), report.getMessageSnapshot(), report.getCreatedAt());
    }
}
