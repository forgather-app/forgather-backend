package com.forgather.domain.notice.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notice.model.Notice;

public record NoticeDetailResponse(
    Long id,
    String title,
    String body,
    LocalDateTime createdAt
) {

    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(notice.getId(), notice.getTitle(), notice.getBody(), notice.getCreatedAt());
    }
}
