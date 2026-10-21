package com.forgather.global.external.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlaceResult(
    @Schema(description = "장소 ID")
    String placeId,

    @Schema(description = "장소명")
    String placeName,

    @Schema(description = "장소 상세 페이지 URL")
    String placeUrl,

    @Schema(description = "지번 주소, 검색 제공자의 원본 값", nullable = true)
    String address,

    @Schema(description = "도로명 주소, 검색 제공자의 원본 값", nullable = true)
    String roadAddress
) {
}
