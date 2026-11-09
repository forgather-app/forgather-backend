package com.forgather.back_office.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

public record AdminProductDetailResponse(
    Long id,
    String title,
    String authorName,
    String description,
    String videoUrl,
    Boolean isVideoAfterPhoto,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    SimpleSpaceResponse space,
    List<AdminProductPhotoResponse> photos
) {

    public static AdminProductDetailResponse of(Product product, List<ProductPhoto> photos) {
        return new AdminProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getAuthorName(),
            product.getDescription(),
            product.getVideoUrl(),
            product.isVideoAfterPhoto(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            SimpleSpaceResponse.from(product.getSpace()),
            photos.stream().map(AdminProductPhotoResponse::from).toList()
        );
    }
}
