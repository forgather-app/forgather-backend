package com.forgather.domain.space.dto;

import java.time.LocalDate;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.forgather.global.validation.TextSize;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateSpaceRequest(

    @Schema(description = "새로운 스페이스 이름", example = "나의 졸업전시", maxLength = 30, nullable = true)
    @TextSize(max = 30)
    String name,

    @Schema(description = "새로운 스페이스 설명", example = "졸업전시 스페이스입니다.", maxLength = 500, nullable = true)
    @TextSize(max = 500)
    String description,

    @Schema(description = "새로운 스페이스 공개여부", example = "true", nullable = true)
    Boolean isPublic,

    @Schema(
        description = "새로운 소개 링크 URL (빈 문자열로 삭제, 삭제 시 표시 이름도 함께 비워야 함)",
        example = "https://forgather.app",
        maxLength = 2048,
        nullable = true
    )
    @URL
    String linkUrl,

    @Schema(
        description = "새로운 소개 링크 표시 이름 (선택, 링크 URL이 있을 때만 입력 가능)",
        example = "포트폴리오",
        maxLength = 30,
        nullable = true
    )
    String linkName,

    @Schema(description = "전시 시작일 (종료일과 함께 입력, 시작일은 종료일 이하)", example = "2026-10-17", nullable = true)
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    LocalDate exhibitionStartDate,

    @Schema(description = "전시 종료일 (시작일과 함께 입력)", example = "2026-10-31", nullable = true)
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    LocalDate exhibitionEndDate,

    @Schema(description = "전시 기간 삭제 여부. 새 날짜 쌍이 있으면 교체가 우선하며, 날짜가 없을 때 true면 삭제한다. false, null, 생략이면 유지한다.",
        example = "false", nullable = true)
    Boolean isDeleteExhibitionPeriod
) {
}
