package com.forgather.domain.space.dto;

import org.hibernate.validator.constraints.URL;

import com.forgather.domain.space.model.Space;
import com.forgather.global.validation.TextSize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateSpaceRequest(

    @Schema(description = "스페이스 이름", example = "졸업 전시", maxLength = 30)
    @NotBlank
    @TextSize(max = 30)
    String name,

    @Schema(description = "스페이스 설명", example = "스페이스 설명", maxLength = 200, nullable = true)
    @TextSize(max = 200)
    String description,

    @Schema(description = "스페이스 공개 여부", example = "true")
    boolean isPublic,

    @Schema(description = "스페이스 호스트 인스타그램 아이디", example = "forgather_official", maxLength = 30, nullable = true)
    @TextSize(max = 30)
    String instagramUsername,

    @Schema(description = "스페이스 호스트 이메일", example = "forgather@forgather.me", maxLength = 50, nullable = true)
    @Email
    @TextSize(max = 30)
    String email,

    @Schema(
        description = "스페이스 소개 링크 URL (표시 이름과 함께 입력)",
        example = "https://forgather.app",
        maxLength = 2048,
        nullable = true
    )
    @URL
    String linkUrl,

    @Schema(
        description = "스페이스 소개 링크 표시 이름 (URL과 함께 입력)",
        example = "포트폴리오",
        maxLength = 30,
        nullable = true
    )
    String linkName
) {

    public Space toEntity(String spaceCode) {
        return new Space(spaceCode, name, description, isPublic, instagramUsername, email, linkUrl, linkName);
    }
}
