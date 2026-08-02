package com.forgather.domain.space.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeatureSpacesRequest(

    @Schema(description = "'지금 축하받고 있는 스페이스'로 지정할 스페이스 코드 목록. "
        + "요청에 포함되지 않은 스페이스의 지정 상태는 변경되지 않습니다.",
        example = "[\"1234567890\", \"0987654321\"]")
    @NotNull
    List<@NotBlank String> spaceCodes
) {

    public Set<String> toUniqueSpaceCodes() {
        if (spaceCodes == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(spaceCodes);
    }
}
