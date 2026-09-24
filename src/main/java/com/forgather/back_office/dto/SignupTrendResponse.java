package com.forgather.back_office.dto;

import java.util.List;

import com.forgather.back_office.model.TrendPoint;
import com.forgather.back_office.model.TrendUnit;

public record SignupTrendResponse(
    String unit,
    long totalCount,
    List<TrendPoint> points
) {

    public static SignupTrendResponse of(TrendUnit unit, List<TrendPoint> points) {
        long totalCount = points.stream().mapToLong(TrendPoint::count).sum();
        return new SignupTrendResponse(unit.name(), totalCount, points);
    }
}
