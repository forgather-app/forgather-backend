package com.forgather.domain.exhibition.dto;

import java.time.LocalDate;
import java.time.ZoneId;

public enum ExhibitionStatus {
    UPCOMING,
    IN_PROGRESS,
    ENDED;

    public static ExhibitionStatus from(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (today.isBefore(startDate)) {
            return UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return ENDED;
        }
        return IN_PROGRESS;
    }
}
