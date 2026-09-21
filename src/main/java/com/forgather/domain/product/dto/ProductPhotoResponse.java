package com.forgather.domain.product.dto;

import com.forgather.domain.product.model.ProductPhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductPhotoResponse(
    @Schema(description = "사진 id", example = "1")
    Long id,

    @Schema(description = "원본 파일명", example = "chair1.jpg")
    String originalName,

    @Schema(description = "S3 파일 경로", example = "images/prod/spaces/1234567890/product/abc.jpg")
    String path,

    @Schema(description = "사진 정렬 순서", example = "1")
    int order
) {

    public ProductPhotoResponse(ProductPhoto photo) {
        this(photo.getId(), photo.getOriginalName(), photo.getPath(), photo.getSortOrder());
    }
}
