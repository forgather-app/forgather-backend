package com.forgather.domain.exhibition.dto;

import java.util.List;

import com.forgather.domain.exhibition.model.ExhibitionTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record OperatingHoursResponse(

    @Schema(
        description = "운영하는 요일별 시간 목록. 운영 요일만 MONDAY~SUNDAY 순으로 포함된다.",
        example = """
            [
                {
                    "dayOfWeek": "MONDAY",
                    "startTime": "10:00",
                    "endTime": "18:00"
                },
                {
                    "dayOfWeek": "TUESDAY",
                    "startTime": "10:00",
                    "endTime": "18:00"
                }
            ]""")
    List<TimeRangeResponse> timeRanges
) {

    public static OperatingHoursResponse from(List<ExhibitionTime> times) {
        return new OperatingHoursResponse(
            times.stream()
                .map(TimeRangeResponse::from)
                .toList()
        );
    }
}
