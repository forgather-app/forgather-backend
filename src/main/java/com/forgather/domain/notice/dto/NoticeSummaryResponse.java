package com.forgather.domain.notice.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notice.model.Notice;

public record NoticeSummaryResponse(
    Long id,
    String title,
    LocalDateTime createdAt
) {

    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(notice.getId(), notice.getTitle(), notice.getCreatedAt());
    }
}
