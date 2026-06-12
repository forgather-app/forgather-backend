package com.forgather.domain.space.dto;

import org.hibernate.validator.constraints.URL;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

public record UpdateSpaceRequest(

    @Schema(description = "새로운 스페이스 이름", example = "나의 졸업전시", maxLength = 30, nullable = true)
    String name,

    @Schema(description = "새로운 스페이스 설명", example = "졸업전시 스페이스입니다.", maxLength = 200, nullable = true)
    String description,

    @Schema(description = "새로운 스페이스 공개여부", example = "true", nullable = true)
    Boolean isPublic,

    @Schema(description = "새로운 인스타그램 아이디", example = "forgather_official_new", maxLength = 30, nullable = true)
    String instagramUsername,

    @Schema(description = "새로운 이메일", example = "forgather_new@forgather.me", maxLength = 50, nullable = true)
    @Email
    String email,

    @Schema(description = "새로운 소개 링크 URL (표시 이름과 함께 입력, 빈 쌍으로 삭제)", example = "https://forgather.me", maxLength = 2048, nullable = true)
    @URL
    String linkUrl,

    @Schema(description = "새로운 소개 링크 표시 이름 (URL과 함께 입력)", example = "포트폴리오", maxLength = 30, nullable = true)
    String linkName,

    @Schema(description = "스페이스 사진 삭제 여부", example = "true")
    Boolean isDeletePhoto
) {

    public boolean isDeletingPhoto() {
        return isDeletePhoto != null && isDeletePhoto;
    }
}
