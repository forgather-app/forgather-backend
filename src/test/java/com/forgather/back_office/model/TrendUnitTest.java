package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrendUnitTest {

    // 2026-09-23은 수요일
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);

    @DisplayName("주 단위 구간 시작일은 해당 주의 월요일이다.")
    @Test
    void weekPeriodStartIsMonday() {
        assertThat(TrendUnit.WEEK.periodStartOf(TODAY)).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(TrendUnit.WEEK.periodStartOf(LocalDate.of(2026, 9, 20))).isEqualTo(LocalDate.of(2026, 9, 14));
    }

    @DisplayName("월 단위 구간 시작일은 해당 월의 1일이다.")
    @Test
    void monthPeriodStartIsFirstDay() {
        assertThat(TrendUnit.MONTH.periodStartOf(TODAY)).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @DisplayName("일 단위는 오늘을 포함한 최근 30일을 오래된 순으로 반환한다.")
    @Test
    void dayPeriodStarts() {
        List<LocalDate> result = TrendUnit.DAY.periodStarts(TODAY);

        assertThat(result).hasSize(30);
        assertThat(result.get(0)).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(result.get(29)).isEqualTo(TODAY);
    }

    @DisplayName("주 단위는 이번 주를 포함한 최근 12주를 오래된 순으로 반환한다.")
    @Test
    void weekPeriodStarts() {
        List<LocalDate> result = TrendUnit.WEEK.periodStarts(TODAY);

        assertThat(result).hasSize(12);
        assertThat(result.get(0)).isEqualTo(LocalDate.of(2026, 7, 6));
        assertThat(result.get(11)).isEqualTo(LocalDate.of(2026, 9, 21));
    }

    @DisplayName("월 단위는 이번 달을 포함한 최근 12개월을 오래된 순으로 반환한다.")
    @Test
    void monthPeriodStarts() {
        List<LocalDate> result = TrendUnit.MONTH.periodStarts(TODAY);

        assertThat(result).hasSize(12);
        assertThat(result.get(0)).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(result.get(11)).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @DisplayName("일별 개수를 주 단위 구간으로 합산하고 빈 구간은 0으로 채운다.")
    @Test
    void rollUpWeek() {
        List<DailyCount> dailyCounts = List.of(
            new DailyCount(LocalDate.of(2026, 9, 21), 2), // 월
            new DailyCount(LocalDate.of(2026, 9, 23), 3), // 수
            new DailyCount(LocalDate.of(2026, 9, 20), 1)  // 지난주 일
        );

        List<TrendPoint> result = TrendUnit.WEEK.rollUp(dailyCounts, TrendUnit.WEEK.periodStarts(TODAY));

        assertThat(result).hasSize(12);
        assertThat(result.get(11)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 21), 5));
        assertThat(result.get(10)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 14), 1));
        assertThat(result.get(0)).isEqualTo(new TrendPoint(LocalDate.of(2026, 7, 6), 0));
    }

    @DisplayName("조회 구간에 속하지 않는 일자는 롤업에서 무시한다.")
    @Test
    void rollUpIgnoresOutOfRange() {
        List<DailyCount> dailyCounts = List.of(new DailyCount(LocalDate.of(2026, 8, 24), 7));

        List<TrendPoint> result = TrendUnit.DAY.rollUp(dailyCounts, TrendUnit.DAY.periodStarts(TODAY));

        assertThat(result).extracting(TrendPoint::count).containsOnly(0L);
    }
}
