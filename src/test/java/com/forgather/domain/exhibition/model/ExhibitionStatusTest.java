package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExhibitionStatusTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 5, 10);
    private static final LocalDate END_DATE = LocalDate.of(2026, 5, 20);

    @DisplayName("기준 날짜가 시작 날짜 이전이면 UPCOMING 이다.")
    @Test
    void upcomingWhenBeforeStartDate() {
        // given
        LocalDate today = START_DATE.minusDays(1);

        // when
        ExhibitionStatus status = ExhibitionStatus.of(START_DATE, END_DATE, today);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.UPCOMING);
    }

    @DisplayName("기준 날짜가 시작 날짜와 같으면 IN_PROGRESS 이다.")
    @Test
    void inProgressOnStartDate() {
        // when
        ExhibitionStatus status = ExhibitionStatus.of(START_DATE, END_DATE, START_DATE);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.IN_PROGRESS);
    }

    @DisplayName("기준 날짜가 시작 날짜와 종료 날짜 사이이면 IN_PROGRESS 이다.")
    @Test
    void inProgressBetweenStartAndEndDate() {
        // given
        LocalDate today = LocalDate.of(2026, 5, 15);

        // when
        ExhibitionStatus status = ExhibitionStatus.of(START_DATE, END_DATE, today);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.IN_PROGRESS);
    }

    @DisplayName("기준 날짜가 종료 날짜와 같으면 IN_PROGRESS 이다.")
    @Test
    void inProgressOnEndDate() {
        // when
        ExhibitionStatus status = ExhibitionStatus.of(START_DATE, END_DATE, END_DATE);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.IN_PROGRESS);
    }

    @DisplayName("기준 날짜가 종료 날짜 이후이면 ENDED 이다.")
    @Test
    void endedWhenAfterEndDate() {
        // given
        LocalDate today = END_DATE.plusDays(1);

        // when
        ExhibitionStatus status = ExhibitionStatus.of(START_DATE, END_DATE, today);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.ENDED);
    }

    @DisplayName("시작 날짜와 종료 날짜가 같은 당일 전시는 기준 날짜가 당일이면 IN_PROGRESS 이다.")
    @Test
    void inProgressOnSingleDayExhibition() {
        // given
        LocalDate singleDay = LocalDate.of(2026, 5, 12);

        // when
        ExhibitionStatus status = ExhibitionStatus.of(singleDay, singleDay, singleDay);

        // then
        assertThat(status).isEqualTo(ExhibitionStatus.IN_PROGRESS);
    }
}
