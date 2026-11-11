package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.product.model.ProductPhoto;

/**
 * 어드민 작품 상세의 사진 항목. 어드민 화면은 사진을 직접 띄우지 않고 게스트 페이지로 이동해 확인하므로 S3 경로는 내려주지 않는다.
 */
public record AdminProductPhotoResponse(
    Long id,
    int sortOrder,
    String originalName,
    Long capacity,
    LocalDateTime createdAt
) {

    public static AdminProductPhotoResponse from(ProductPhoto photo) {
        return new AdminProductPhotoResponse(
            photo.getId(),
            photo.getSortOrder(),
            photo.getOriginalName(),
            photo.getCapacity(),
            photo.getCreatedAt()
        );
    }
}
