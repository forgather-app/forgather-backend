package com.forgather.global.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingRequest(
    @Schema(description = "서비스 닉네임", example = "포개더")
    String nickname,

    @Schema(description = "동의한 약관 ID 목록", example = "[1, 2]")
    List<Long> agreedTermIds,

    @Schema(description = "거절한 약관 ID 목록. 모든 약관 타입에 대해 동의 또는 거절 결정이 명시되어야 하며, "
        + "필수 약관은 거절할 수 없습니다.", example = "[3]")
    List<Long> rejectedTermIds
) {

    public OnboardingRequest {
        if (rejectedTermIds == null) {
            rejectedTermIds = List.of();
        }
    }
}
