package com.forgather.back_office.model;

import java.time.LocalDate;

public record TrendPoint(LocalDate periodStart, long count) {
}
