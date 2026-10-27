package com.forgather.domain.space.model;

import java.time.LocalDate;

public enum GuestBookWritingPolicy {

    ALWAYS,
    EXHIBITION_PERIOD,
    ;

    public boolean isWritable(SpaceExhibitionPeriod period, LocalDate date) {
        return switch (this) {
            case ALWAYS -> true;
            case EXHIBITION_PERIOD -> period != null && period.isOngoing(date);
        };
    }
}
