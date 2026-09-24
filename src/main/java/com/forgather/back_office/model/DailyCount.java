package com.forgather.back_office.model;

import java.time.LocalDate;

public record DailyCount(LocalDate day, long count) {
}
