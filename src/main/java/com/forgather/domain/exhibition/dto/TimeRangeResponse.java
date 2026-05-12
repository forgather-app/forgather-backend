package com.forgather.domain.exhibition.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.forgather.domain.exhibition.model.ExhibitionTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimeRangeResponse(

    @Schema(
        description = "운영 요일",
        example = "MONDAY",
        allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}
    )
    DayOfWeek dayOfWeek,

    @Schema(description = "운영 시작 시간", example = "10:00")
    LocalTime startTime,

    @Schema(description = "운영 종료 시간", example = "18:00")
    LocalTime endTime
) {
    public static TimeRangeResponse from(ExhibitionTime time) {
        return new TimeRangeResponse(time.getDayOfWeek(), time.getStartTime(), time.getEndTime());
    }
}
