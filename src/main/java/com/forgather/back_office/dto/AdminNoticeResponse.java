package com.forgather.back_office.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.forgather.domain.notice.model.Notice;

public record AdminNoticeResponse(
    List<NoticeSummaryResponse> notices,
    int currentPage,
    int pageSize,
    long totalCount,
    int totalPages
) {

    public static AdminNoticeResponse from(Page<Notice> notices) {
        return new AdminNoticeResponse(notices.map(NoticeSummaryResponse::from).toList(),
            notices.getNumber() + 1, notices.getSize(), notices.getTotalElements(), notices.getTotalPages());
    }
}
