package com.forgather.domain.exhibition.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.forgather.domain.exhibition.model.ExhibitionTime;

public record TimeRangeResponse(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
    public static TimeRangeResponse from(ExhibitionTime time) {
        return new TimeRangeResponse(time.getDayOfWeek(), time.getStartTime(), time.getEndTime());
    }
}
