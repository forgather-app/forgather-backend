package com.forgather.domain.space.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CelebratingSpaceResponse(

    @Schema(description = "'지금 축하받고 있는 스페이스'로 지정된 스페이스 코드", example = "1234567890")
    String spaceCode
) {
}
