package com.forgather.domain.exhibition.dto;

import java.util.List;

import com.forgather.domain.exhibition.model.ExhibitionTime;

public record OperatingHoursResponse(List<TimeRangeResponse> timeRanges) {

    public static OperatingHoursResponse from(List<ExhibitionTime> times) {
        return new OperatingHoursResponse(
            times.stream()
                .map(TimeRangeResponse::from)
                .toList()
        );
    }
}
