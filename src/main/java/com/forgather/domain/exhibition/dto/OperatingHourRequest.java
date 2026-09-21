package com.forgather.domain.exhibition.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OperatingHourRequest(

    @Schema(
        description = "운영 요일",
        example = "MONDAY",
        allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}
    )
    @NotNull
    DayOfWeek dayOfWeek,

    @Schema(description = "운영 시작 시간", example = "10:00")
    @NotNull
    LocalTime startTime,

    @Schema(description = "운영 종료 시간", example = "18:00")
    @NotNull
    LocalTime endTime
) {
}
