package com.forgather.back_office.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminProductResponse(
    List<AdminProductSummaryResponse> products,
    int currentPage,
    int pageSize,
    long totalCount,
    int totalPages
) {

    public static AdminProductResponse from(Page<AdminProductSummaryResponse> products) {
        return new AdminProductResponse(
            products.getContent(),
            products.getNumber() + 1,
            products.getSize(),
            products.getTotalElements(),
            products.getTotalPages()
        );
    }
}
