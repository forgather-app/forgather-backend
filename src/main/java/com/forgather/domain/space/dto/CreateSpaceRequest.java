package com.forgather.domain.space.dto;

import java.time.LocalDate;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.forgather.domain.space.model.Space;
import com.forgather.global.validation.TextSize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateSpaceRequest(

    @Schema(description = "스페이스 이름", example = "졸업 전시", maxLength = 30)
    @NotBlank
    @TextSize(max = 30)
    String name,

    @Schema(description = "스페이스 설명", example = "스페이스 설명", maxLength = 500, nullable = true)
    @TextSize(max = 500)
    String description,

    @Schema(description = "스페이스 공개 여부", example = "true", defaultValue = "false")
    boolean isPublic,

    @Schema(
        description = "스페이스 소개 링크 URL (선택)",
        example = "https://forgather.app",
        maxLength = 2048,
        nullable = true
    )
    @URL
    @TextSize(max = 2048)
    String linkUrl,

    @Schema(
        description = "스페이스 소개 링크 표시 이름 (선택, 링크 URL이 있을 때만 입력 가능)",
        example = "포트폴리오",
        maxLength = 30,
        nullable = true
    )
    @TextSize(max = 30)
    String linkName,

    @Schema(description = "전시 시작일 (종료일과 함께 입력, 시작일은 종료일 이하)", example = "2026-10-17", nullable = true)
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    LocalDate exhibitionStartDate,

    @Schema(description = "전시 종료일 (시작일과 함께 입력)", example = "2026-10-31", nullable = true)
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    LocalDate exhibitionEndDate,

    @Schema(
        description = "장소명 (선택, 최대 50자, 장소명만 입력 가능). 생성 시 null, 빈 문자열, 공백만 있는 값은 빈 문자열로 저장. 값이 있는 문자열의 앞뒤 공백은 보존",
        example = "포게더 전시관",
        maxLength = 50,
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String locationName,

    @Schema(
        description = "카카오 장소 ID (선택, 숫자 문자열, 장소명 필수, URL과 쌍으로 입력). 생성 시 null, 빈 문자열, 공백만 있는 값은 빈 문자열로 저장",
        example = "123456789",
        maxLength = 255,
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String kakaoPlaceId,

    @Schema(
        description = "카카오 장소 URL (선택, 장소명 필수, ID와 쌍으로 입력). https://place.map.kakao.com/ + ID와 정확히 일치해야 함. 생성 시 null, 빈 문자열, 공백만 있는 값은 빈 문자열로 저장",
        example = "https://place.map.kakao.com/123456789",
        maxLength = 2048,
        nullable = true,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String kakaoPlaceUrl
) {

    public Space toEntity(String spaceCode) {
        return new Space(spaceCode, name, description, isPublic, linkUrl, linkName,
            exhibitionStartDate, exhibitionEndDate, locationName, kakaoPlaceId, kakaoPlaceUrl);
    }
}
