package com.forgather.domain.space.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateFeaturedSpaceRequest(

    @Schema(description = "'지금 축하받고 있는 스페이스'로 지정할 스페이스 코드", example = "1234567890")
    @NotBlank
    String spaceCode
) {
}
