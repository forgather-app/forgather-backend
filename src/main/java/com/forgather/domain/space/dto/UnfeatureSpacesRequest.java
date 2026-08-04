package com.forgather.domain.space.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnfeatureSpacesRequest(

    @Schema(description = "'지금 축하받고 있는 스페이스' 지정을 해제할 스페이스 코드 목록. 최대 100개. "
        + "요청에 포함되지 않은 스페이스의 지정 상태는 변경되지 않습니다.",
        example = "[\"1234567890\", \"0987654321\"]")
    @NotNull
    @Size(max = 100, message = "스페이스 코드는 최대 100개까지 요청할 수 있습니다.")
    List<@NotBlank String> spaceCodes
) {

    public Set<String> toUniqueSpaceCodes() {
        return new HashSet<>(spaceCodes);
    }
}
