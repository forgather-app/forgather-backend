package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;

class MonthRangeTest {

    @DisplayName("시작 월부터 종료 월까지 각 월의 1일을 오래된 순으로 반환한다.")
    @Test
    void periodStarts() {
        MonthRange range = new MonthRange(YearMonth.of(2025, 11), YearMonth.of(2026, 2));

        assertThat(range.periodStarts()).containsExactly(
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1)
        );
    }

    @DisplayName("시작 월과 종료 월이 같으면 한 달을 조회한다.")
    @Test
    void singleMonth() {
        MonthRange range = new MonthRange(YearMonth.of(2026, 9), YearMonth.of(2026, 9));

        assertThat(range.periodStarts()).containsExactly(LocalDate.of(2026, 9, 1));
    }

    @DisplayName("시작 월과 종료 월 중 하나라도 없으면 예외가 발생한다.")
    @Test
    void requiresBoth() {
        assertThatThrownBy(() -> new MonthRange(YearMonth.of(2026, 1), null))
            .isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> new MonthRange(null, YearMonth.of(2026, 1)))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("시작 월이 종료 월보다 늦으면 예외가 발생한다.")
    @Test
    void fromAfterTo() {
        assertThatThrownBy(() -> new MonthRange(YearMonth.of(2026, 5), YearMonth.of(2026, 4)))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("12개월을 넘는 기간도 조회할 수 있다.")
    @Test
    void longerThanTwelveMonths() {
        MonthRange range = new MonthRange(YearMonth.of(2025, 7), YearMonth.of(2026, 9));

        assertThat(range.periodStarts()).hasSize(15);
    }

    @DisplayName("2025년 7월 이전부터는 조회할 수 없다.")
    @Test
    void minMonth() {
        assertThatCode(() -> new MonthRange(YearMonth.of(2025, 7), YearMonth.of(2025, 9)))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> new MonthRange(YearMonth.of(2025, 6), YearMonth.of(2025, 9)))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("종료 월이 기준 월보다 미래이면 예외가 발생한다.")
    @Test
    void validateNotAfter() {
        MonthRange range = new MonthRange(YearMonth.of(2026, 9), YearMonth.of(2026, 10));

        assertThatThrownBy(() -> range.validateNotAfter(YearMonth.of(2026, 9)))
            .isInstanceOf(BaseException.class);
        assertThatCode(() -> range.validateNotAfter(YearMonth.of(2026, 10)))
            .doesNotThrowAnyException();
    }
}
