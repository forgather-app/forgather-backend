package com.forgather.global.auth.dto;

import com.forgather.global.auth.model.AppUser;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppUserResponse(

    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 프로필 사진 URL", example = "https://example.com/profile.jpg")
    String pictureUrl,

    @Schema(description = "약관 동의 여부", example = "true")
    boolean agreedTerms
) {

    public static AppUserResponse from(AppUser appUser) {
        return new AppUserResponse(
            appUser.getId(),
            appUser.getName(),
            appUser.getPictureUrl(),
            appUser.getAgreedTerms()
        );
    }
}
