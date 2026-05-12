package com.forgather.domain.exhibition.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExhibitionRequest(

    @Schema(description = "전시 이미지 경로 (S3 key)", example = "exhibitions/abc.webp")
    @NotBlank
    String imagePath,

    @Schema(description = "전시 이미지 파일 크기 (bytes)", example = "102400")
    @NotNull
    Long imageCapacity,

    @Schema(description = "전시 제목 (최대 100자)", example = "봄 전시")
    @NotBlank
    String title,

    @Schema(description = "시작일", example = "2026-06-01")
    @NotNull
    LocalDate startDate,

    @Schema(description = "종료일", example = "2026-06-30")
    @NotNull
    LocalDate endDate,

    @Schema(description = "전시 소개 (최대 200자, null 가능)")
    String description,

    @Schema(description = "운영 공지 (최대 200자, null 가능)")
    String operationNotice,

    @Schema(description = "운영하는 요일별 시간. 같은 요일 중복 금지. 최대 7개. (null 가능)")
    @Valid
    @Size(max = 7, message = "운영시간은 최대 7개입니다.")
    List<OperatingHourRequest> operatingHours,

    @Schema(description = "장소 정보 (null 가능)")
    @Valid
    LocationRequest location
) {

    public CreateExhibitionRequest {
        operatingHours = operatingHours == null ? List.of() : operatingHours;
    }
}
