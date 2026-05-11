package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.LocationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(

    @Schema(description = "장소 타입", example = "ONLINE")
    @NotNull
    LocationType locationType,

    @Schema(description = "온라인 URL (ONLINE 타입)")
    String url,

    @Schema(description = "기본 주소 (OFFLINE 타입)")
    String baseAddress,

    @Schema(description = "상세 주소 (OFFLINE 타입)")
    String detailAddress
) {
}
