package com.forgather.domain.space.dto;

import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.space.model.Space;

import io.swagger.v3.oas.annotations.media.Schema;

public record HostSpaceItemResponse(

    @Schema(description = "스페이스 ID", example = "1")
    Long id,

    @Schema(description = "스페이스 코드", example = "1234567890")
    String spaceCode,

    @Schema(description = "스페이스 이름", example = "My Space")
    String name,

    @Schema(description = "스페이스 설명", example = "나의 졸업 전시.")
    String description,

    @Schema(description = "스페이스 공개여부", example = "true")
    boolean isPublic,

    @Schema(description = "스페이스 소개 링크 URL", example = "https://forgather.app")
    String linkUrl,

    @Schema(description = "스페이스 소개 링크 표시 이름", example = "포트폴리오")
    String linkName,

    @Schema(description = "스페이스 사진 경로 (대표 작품의 첫 번째 사진). 작품이 없거나 대표 작품에 사진이 없으면 null이며, 기본 사진 노출은 클라이언트가 담당한다.",
        example = "images/prod/spaces/1234567890/product/UUID.webp", nullable = true)
    String spacePhotoPath,

    @Schema(description = "스페이스 방명록 카드 개수", example = "15")
    Long guestBookCardCount,

    @Schema(description = "호스트가 아직 읽지 않은 방명록 카드 개수", example = "6")
    Long unreadGuestBookCount,

    @Schema(description = "'지금 축하받고 있는 스페이스'로 지정되었는지 여부", example = "true")
    boolean isFeatured
) {

    public static HostSpaceItemResponse from(
        Space space,
        ProductPhoto spacePhoto,
        Long guestBookCardCount,
        Long unreadGuestBookCount
    ) {
        return new HostSpaceItemResponse(
            space.getId(),
            space.getCode(),
            space.getName(),
            space.getDescription(),
            space.isPublic(),
            space.getLinkUrl(),
            space.getLinkName(),
            (spacePhoto == null) ? null : spacePhoto.getPath(),
            guestBookCardCount,
            unreadGuestBookCount,
            space.isFeatured()
        );
    }
}
