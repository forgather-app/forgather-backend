package com.forgather.back_office.model;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrendUnit {

    DAY(30) {
        @Override
        public LocalDate periodStartOf(LocalDate date) {
            return date;
        }

        @Override
        protected LocalDate minusPeriods(LocalDate periodStart, long periods) {
            return periodStart.minusDays(periods);
        }
    },
    WEEK(12) {
        @Override
        public LocalDate periodStartOf(LocalDate date) {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        @Override
        protected LocalDate minusPeriods(LocalDate periodStart, long periods) {
            return periodStart.minusWeeks(periods);
        }
    },
    MONTH(12) {
        @Override
        public LocalDate periodStartOf(LocalDate date) {
            return date.withDayOfMonth(1);
        }

        @Override
        protected LocalDate minusPeriods(LocalDate periodStart, long periods) {
            return periodStart.minusMonths(periods);
        }
    };

    private final int periodCount;

    public abstract LocalDate periodStartOf(LocalDate date);

    protected abstract LocalDate minusPeriods(LocalDate periodStart, long periods);

    /**
     * 오늘이 속한 구간을 포함해 최근 periodCount개 구간의 시작일을 오래된 순으로 반환한다.
     */
    public List<LocalDate> periodStarts(LocalDate today) {
        LocalDate current = periodStartOf(today);
        return IntStream.range(0, periodCount)
            .mapToObj(i -> minusPeriods(current, periodCount - 1 - i))
            .toList();
    }

    /**
     * 일별 개수를 주어진 구간별로 합산한다. 개수가 없는 구간은 0으로 채우고, 조회 구간 밖의 일자는 무시한다.
     */
    public List<TrendPoint> rollUp(List<DailyCount> dailyCounts, List<LocalDate> periodStarts) {
        Map<LocalDate, Long> countByPeriod = dailyCounts.stream()
            .collect(groupingBy(dailyCount -> periodStartOf(dailyCount.day()), summingLong(DailyCount::count)));

        return periodStarts.stream()
            .map(periodStart -> new TrendPoint(periodStart, countByPeriod.getOrDefault(periodStart, 0L)))
            .toList();
    }
}
