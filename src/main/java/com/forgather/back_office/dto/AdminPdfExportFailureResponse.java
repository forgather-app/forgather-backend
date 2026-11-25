package com.forgather.back_office.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminPdfExportFailureResponse(
    List<AdminPdfExportFailureSummaryResponse> failures,
    int currentPage,
    int pageSize,
    long totalCount,
    int totalPages
) {

    public static AdminPdfExportFailureResponse from(Page<AdminPdfExportFailureSummaryResponse> page) {
        return new AdminPdfExportFailureResponse(page.getContent(), page.getNumber() + 1,
            page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
