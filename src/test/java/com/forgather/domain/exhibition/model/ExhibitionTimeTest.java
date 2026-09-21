package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.fixture.ExhibitionFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class ExhibitionTimeTest {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("전시가 null이면 생성할 수 없다.")
        @Test
        void createWithNullExhibition() {
            // when & then
            assertThatThrownBy(() -> new ExhibitionTime(null, DayOfWeek.MONDAY, DEFAULT_START_TIME, DEFAULT_END_TIME))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("전시");
        }

        @DisplayName("운영 요일이 null이면 생성할 수 없다.")
        @Test
        void createWithNullDayOfWeek() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

            // when & then
            assertThatThrownBy(() -> new ExhibitionTime(
                exhibition, null, DEFAULT_START_TIME, DEFAULT_END_TIME))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("운영 요일");
        }

        @DisplayName("시작 시간이 null이면 생성할 수 없다.")
        @Test
        void createWithNullStartTime() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

            // when & then
            assertThatThrownBy(() -> new ExhibitionTime(exhibition, DayOfWeek.MONDAY, null, DEFAULT_END_TIME))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("시작 시간");
        }

        @DisplayName("종료 시간이 null이면 생성할 수 없다.")
        @Test
        void createWithNullEndTime() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

            // when & then
            assertThatThrownBy(() -> new ExhibitionTime(exhibition, DayOfWeek.MONDAY, DEFAULT_START_TIME, null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("종료 시간");
        }
    }

    @Nested
    @DisplayName("운영 시간 검증")
    class TimeValidation {

        @DisplayName("시작 시간이 종료 시간보다 늦으면 생성할 수 없다.")
        @Test
        void createWithStartTimeAfterEndTime() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();
            LocalTime startTime = LocalTime.of(18, 0);
            LocalTime endTime = LocalTime.of(10, 0);

            // when & then
            assertThatThrownBy(() -> new ExhibitionTime(exhibition, DayOfWeek.MONDAY, startTime, endTime))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("시작 시간");
        }

        @DisplayName("시작 시간과 종료 시간이 같으면 생성할 수 있다.")
        @Test
        void createWithSameStartAndEndTime() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();
            LocalTime sameTime = LocalTime.of(12, 0);

            // when
            ExhibitionTime exhibitionTime = new ExhibitionTime(exhibition, DayOfWeek.MONDAY, sameTime, sameTime);

            // then
            assertThat(exhibitionTime.getStartTime()).isEqualTo(exhibitionTime.getEndTime());
        }
    }

    @DisplayName("유효한 값으로 운영 시간을 생성할 수 있다.")
    @Test
    void createWithValidFields() {
        // given
        Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

        // when
        ExhibitionTime exhibitionTime = new ExhibitionTime(
            exhibition, DayOfWeek.WEDNESDAY, DEFAULT_START_TIME, DEFAULT_END_TIME);

        // then
        assertAll(
            () -> assertThat(exhibitionTime.getExhibition()).isEqualTo(exhibition),
            () -> assertThat(exhibitionTime.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY),
            () -> assertThat(exhibitionTime.getStartTime()).isEqualTo(DEFAULT_START_TIME),
            () -> assertThat(exhibitionTime.getEndTime()).isEqualTo(DEFAULT_END_TIME)
        );
    }
}
