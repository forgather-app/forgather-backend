package com.forgather.domain.space.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.forgather.global.exception.BaseException;

class SpaceExhibitionPeriodTest {

    @DisplayName("과거와 미래 및 같은 날짜의 전시 기간을 생성할 수 있다")
    @CsvSource({
        "2000-01-01, 2000-01-31",
        "2100-01-01, 2100-01-31",
        "2026-10-19, 2026-10-19",
        "2000-01-01, 2100-01-31"
    })
    @ParameterizedTest
    void createValidPeriod(LocalDate startDate, LocalDate endDate) {
        // given & when
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(startDate, endDate);

        // then
        assertAll(
            () -> assertThat(period.getStartDate()).isEqualTo(startDate),
            () -> assertThat(period.getEndDate()).isEqualTo(endDate)
        );
    }

    @DisplayName("시작일이나 종료일이 없으면 전시 기간 값 객체를 생성할 수 없다")
    @CsvSource(value = {
        "NULL, NULL",
        "2026-10-19, NULL",
        "NULL, 2026-10-19"
    }, nullValues = "NULL")
    @ParameterizedTest
    void rejectsMissingDate(LocalDate startDate, LocalDate endDate) {
        // given & when & then
        assertThatThrownBy(() -> new SpaceExhibitionPeriod(startDate, endDate))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("시작일이 종료일보다 늦으면 전시 기간 값 객체를 생성할 수 없다")
    @Test
    void rejectsReversedPeriod() {
        // given
        LocalDate startDate = LocalDate.of(2026, 10, 20);
        LocalDate endDate = LocalDate.of(2026, 10, 19);

        // when & then
        assertThatThrownBy(() -> new SpaceExhibitionPeriod(startDate, endDate))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("시작일과 종료일이 같은 두 전시 기간은 동일한 값과 해시 코드를 갖는다")
    @Test
    void equalPeriodsHaveEqualHashCodes() {
        // given
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20));
        SpaceExhibitionPeriod other = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20));

        // when & then
        assertAll(
            () -> assertThat(period).isEqualTo(other),
            () -> assertThat(period.hashCode()).isEqualTo(other.hashCode())
        );
    }

    @DisplayName("시작일 또는 종료일이 다른 전시 기간은 서로 다른 값이다")
    @CsvSource({
        "2026-10-18, 2026-10-20",
        "2026-10-19, 2026-10-21"
    })
    @ParameterizedTest
    void differentDatesAreNotEqual(LocalDate startDate, LocalDate endDate) {
        // given
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20));
        SpaceExhibitionPeriod other = new SpaceExhibitionPeriod(startDate, endDate);

        // when & then
        assertThat(period).isNotEqualTo(other);
    }

    @DisplayName("시작일과 종료일을 포함한 날짜에 전시가 진행 중이다")
    @CsvSource({
        "2026-10-19, false",
        "2026-10-20, true",
        "2026-10-22, true",
        "2026-10-24, true",
        "2026-10-25, false"
    })
    @ParameterizedTest
    void checksOngoingIncludingBoundaries(LocalDate date, boolean expected) {
        // given
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24));

        // when
        boolean result = period.isOngoing(date);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("하루 전시는 해당 날짜에만 진행 중이다")
    @CsvSource({"2026-10-21, false", "2026-10-22, true", "2026-10-23, false"})
    @ParameterizedTest
    void checksSingleDayExhibition(LocalDate date, boolean expected) {
        // given
        LocalDate exhibitionDate = LocalDate.of(2026, 10, 22);
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(exhibitionDate, exhibitionDate);

        // when
        boolean result = period.isOngoing(date);

        // then
        assertThat(result).isEqualTo(expected);
    }
}
