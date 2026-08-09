package com.forgather.domain.space.dto;

import com.forgather.domain.product.model.ProductPhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpacePhotoResponse(

    @Schema(description = "스페이스 사진 존재 여부", example = "true")
    boolean isExists,

    @Schema(description = "스페이스 사진 경로", example = "photogather/v2/spaces/1234567890/product/UUID.webp")
    String path
) {

    private static final SpacePhotoResponse EMPTY = new SpacePhotoResponse(false, "");

    /**
     * 스페이스 사진은 대표 작품의 첫 번째 사진이다.
     * 작품이 없거나 대표 작품에 사진이 없으면 사진이 없는 것으로 보고, 기본 사진 노출은 클라이언트가 담당한다.
     *
     * @param photo 대표 작품의 첫 번째 사진 (없으면 null)
     */
    public static SpacePhotoResponse from(ProductPhoto photo) {
        if (photo == null) {
            return EMPTY;
        }
        return new SpacePhotoResponse(true, photo.getPath());
    }
}
