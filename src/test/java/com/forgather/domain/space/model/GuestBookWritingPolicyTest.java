package com.forgather.domain.space.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GuestBookWritingPolicyTest {

    @DisplayName("항상 허용 정책은 전시 기간이 없어도 작성을 허용한다")
    @Test
    void alwaysAllowsWithoutPeriod() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 22);

        // when
        boolean result = GuestBookWritingPolicy.ALWAYS.isWritable(null, date);

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("항상 허용 정책은 전시 기간 전후와 관계없이 작성을 허용한다")
    @CsvSource({"2026-10-19", "2026-10-22", "2026-10-25"})
    @ParameterizedTest
    void alwaysAllowsRegardlessOfPeriod(LocalDate date) {
        // given
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24));

        // when
        boolean result = GuestBookWritingPolicy.ALWAYS.isWritable(period, date);

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("전시 기간 정책은 기간이 없으면 작성을 거절한다")
    @Test
    void rejectsWithoutPeriod() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 22);

        // when
        boolean result = GuestBookWritingPolicy.EXHIBITION_PERIOD.isWritable(null, date);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("전시 기간 정책은 시작일과 종료일을 포함한 기간에만 작성을 허용한다")
    @CsvSource({
        "2026-10-19, false",
        "2026-10-20, true",
        "2026-10-22, true",
        "2026-10-24, true",
        "2026-10-25, false"
    })
    @ParameterizedTest
    void allowsOnlyDuringExhibition(LocalDate date, boolean expected) {
        // given
        SpaceExhibitionPeriod period = new SpaceExhibitionPeriod(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 24));

        // when
        boolean result = GuestBookWritingPolicy.EXHIBITION_PERIOD.isWritable(period, date);

        // then
        assertThat(result).isEqualTo(expected);
    }
}
