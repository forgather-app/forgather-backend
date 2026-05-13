package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.LocationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(

    @Schema(
        description = "장소 타입",
        example = "ONLINE",
        allowableValues = {"ONLINE", "OFFLINE"}
    )
    @NotNull
    LocationType locationType,

    @Schema(description = "온라인 URL (ONLINE 타입일 때 필수)", example = "https://forgather.app")
    String url,

    @Schema(description = "기본 주소 (OFFLINE 타입일 때 필수)", example = "서울특별시 송파구")
    String baseAddress,

    @Schema(description = "상세 주소 (OFFLINE 타입일 때 선택, null 가능)", example = "루터회관")
    String detailAddress
) {
}
