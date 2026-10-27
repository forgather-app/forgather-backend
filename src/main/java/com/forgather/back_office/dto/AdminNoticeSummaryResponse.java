package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notice.model.Notice;

public record AdminNoticeSummaryResponse(
    Long id,
    String title,
    LocalDateTime createdAt
) {

    public static AdminNoticeSummaryResponse from(Notice notice) {
        return new AdminNoticeSummaryResponse(notice.getId(), notice.getTitle(), notice.getCreatedAt());
    }
}
