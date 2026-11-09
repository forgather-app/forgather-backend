package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.domain.product.model.ProductPhoto;

public record AdminProductPhotoResponse(
    Long id,
    int sortOrder,
    String originalName,
    String path,
    Long capacity,
    LocalDateTime createdAt
) {

    public static AdminProductPhotoResponse from(ProductPhoto photo) {
        return new AdminProductPhotoResponse(
            photo.getId(),
            photo.getSortOrder(),
            photo.getOriginalName(),
            photo.getPath(),
            photo.getCapacity(),
            photo.getCreatedAt()
        );
    }
}
