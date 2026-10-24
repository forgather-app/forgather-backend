package com.forgather.domain.notice.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.forgather.domain.notice.model.Notice;

public record NoticesResponse(
    List<NoticeSummaryResponse> notices,
    int currentPage,
    int pageSize,
    long totalCount,
    int totalPages
) {

    public static NoticesResponse from(Page<Notice> notices) {
        return new NoticesResponse(
            notices.getContent().stream().map(NoticeSummaryResponse::from).toList(),
            notices.getNumber() + 1,
            notices.getSize(),
            notices.getTotalElements(),
            notices.getTotalPages()
        );
    }
}
