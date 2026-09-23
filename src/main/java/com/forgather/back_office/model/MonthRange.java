package com.forgather.back_office.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import com.forgather.global.exception.BaseException;

/**
 * 시작 월과 종료 월을 모두 포함하는 조회 기간. 최대 12개월까지 허용한다.
 */
public record MonthRange(YearMonth from, YearMonth to) {

    private static final int MAX_MONTHS = 12;

    public MonthRange {
        if (from == null || to == null) {
            throw new BaseException("조회 기간의 시작 월과 종료 월을 함께 입력해야 합니다.");
        }
        if (from.isAfter(to)) {
            throw new BaseException("시작 월은 종료 월보다 늦을 수 없습니다. from: " + from + ", to: " + to);
        }
        if (monthCount(from, to) > MAX_MONTHS) {
            throw new BaseException("조회 기간은 최대 " + MAX_MONTHS + "개월입니다. from: " + from + ", to: " + to);
        }
    }

    private static long monthCount(YearMonth from, YearMonth to) {
        return from.until(to, ChronoUnit.MONTHS) + 1;
    }

    public void validateNotAfter(YearMonth currentMonth) {
        if (to.isAfter(currentMonth)) {
            throw new BaseException("종료 월은 이번 달 이후일 수 없습니다. to: " + to);
        }
    }

    /**
     * 기간에 포함된 각 월의 1일을 오래된 순으로 반환한다.
     */
    public List<LocalDate> periodStarts() {
        return Stream.iterate(from, month -> month.plusMonths(1))
            .limit(monthCount(from, to))
            .map(month -> month.atDay(1))
            .toList();
    }

    public LocalDate endExclusive() {
        return to.plusMonths(1).atDay(1);
    }
}
