package com.forgather.domain.guestbook.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.forgather.domain.guestbook.model.GuestBookReport;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReportHistoryResponse(
    List<ReportHistoryDto> reportHistory,

    @Schema(description = "현재 페이지 번호", example = "1")
    int currentPage,

    @Schema(description = "페이지 당 방명록 카드 개수", example = "15")
    int pageSize,

    @Schema(description = "총 방명록 카드 개수", example = "3")
    long totalCount,

    @Schema(description = "총 페이지 수", example = "1")
    int totalPages
) {
    public static ReportHistoryResponse from(Page<GuestBookReport> reportHistory) {
        return new ReportHistoryResponse(
            reportHistory.map(ReportHistoryDto::new).toList(),
            reportHistory.getNumber() + 1,
            reportHistory.getSize(),
            reportHistory.getTotalElements(),
            reportHistory.getTotalPages()
        );
    }
}
