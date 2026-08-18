package com.forgather.domain.space.dto;

import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.space.model.Space;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpaceResponse(

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

    @Schema(description = "스페이스 방명록 카드 개수. 비공개 스페이스는 호스트 본인에게만 실제 값을 응답하고 그 외에는 null(개수 비공개)로 응답한다.",
        example = "15", nullable = true)
    Long guestBookCardCount,

    @Schema(description = "스페이스 호스트 정보")
    SpaceHostInfoResponse host
) {

    public static SpaceResponse from(Space space, ProductPhoto spacePhoto, Long guestBookCardCount,
        SpaceHostInfoResponse host) {
        return new SpaceResponse(
            space.getId(),
            space.getCode(),
            space.getName(),
            space.getDescription(),
            space.isPublic(),
            space.getLinkUrl(),
            space.getLinkName(),
            (spacePhoto == null) ? null : spacePhoto.getPath(),
            guestBookCardCount,
            host
        );
    }
}
