package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notice.model.Notice;

public record AdminNoticeDetailResponse(
    Long id,
    String title,
    String body,
    LocalDateTime createdAt
) {

    public static AdminNoticeDetailResponse from(Notice notice) {
        return new AdminNoticeDetailResponse(notice.getId(), notice.getTitle(), notice.getBody(), notice.getCreatedAt());
    }
}
