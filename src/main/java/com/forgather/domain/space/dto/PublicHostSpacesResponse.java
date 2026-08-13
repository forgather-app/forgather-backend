package com.forgather.domain.space.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record PublicHostSpacesResponse(

    @Schema(description = "호스트의 스페이스 목록")
    List<PublicHostSpaceItemResponse> spaces
) {
}
