package com.forgather.global.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingRequest(
    @Schema(description = "서비스 닉네임", example = "포개더")
    String nickname,

    @Schema(description = "동의한 약관 ID 목록", example = "[1, 2, 3]")
    List<Long> agreedTermIds
) {
}
