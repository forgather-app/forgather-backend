package com.forgather.domain.exhibition.model;

import java.time.LocalDate;

public enum ExhibitionStatus {
    UPCOMING,
    IN_PROGRESS,
    ENDED;

    static ExhibitionStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) {
            return UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return ENDED;
        }
        return IN_PROGRESS;
    }
}
