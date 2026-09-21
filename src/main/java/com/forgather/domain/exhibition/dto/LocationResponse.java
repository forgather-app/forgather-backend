package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.Location;
import com.forgather.domain.exhibition.model.LocationType;

import io.swagger.v3.oas.annotations.media.Schema;

public record LocationResponse(

    @Schema(description = "장소 타입", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE"})
    LocationType locationType,

    @Schema(description = "온라인 URL (ONLINE 타입일 때만 값 존재, 그 외 null)", example = "https://forgather.app")
    String url,

    @Schema(description = "기본 주소 (OFFLINE 타입일 때만 값 존재, 그 외 null)", example = "서울특별시 송파구")
    String baseAddress,

    @Schema(description = "상세 주소 (OFFLINE 타입일 때 선택, 미입력 시 null)", example = "루터회관")
    String detailAddress
) {

    public static LocationResponse from(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
            location.getType(),
            location.getOnlineUrl(),
            location.getBaseAddress(),
            location.getDetailAddress()
        );
    }
}
