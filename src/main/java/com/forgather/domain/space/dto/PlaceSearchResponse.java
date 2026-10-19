package com.forgather.domain.space.dto;

import java.util.List;

import com.forgather.global.external.place.dto.PlaceResult;
import com.forgather.global.external.place.dto.PlaceSearchResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlaceSearchResponse(
    @Schema(description = "장소 검색 결과, 결과가 없으면 빈 목록")
    List<PlaceResult> places,

    @Schema(description = "다음 페이지 존재 여부")
    boolean hasNext
) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(result.places(), result.hasNext());
    }
}
