package com.forgather.domain.notice.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.forgather.global.exception.BaseException;

class NoticeTest {

    @DisplayName("필수값이 없거나 표시 글자 제한을 넘으면 공지를 생성할 수 없다")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("invalidNotices")
    void rejectInvalidNotice(String title, String body) {
        // given
        // when & then
        assertThatThrownBy(() -> new Notice(title, body))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getStatusCode()).isEqualTo(400));
    }

    static Stream<Arguments> invalidNotices() {
        return Stream.of(
            Arguments.of(null, "본문"), Arguments.of("", "본문"), Arguments.of(" \n\t", "본문"),
            Arguments.of("제목", null), Arguments.of("제목", ""), Arguments.of("제목", " \n\t"),
            Arguments.of("가".repeat(101), "본문"), Arguments.of("제목", "나".repeat(10001)),
            Arguments.of("👨‍👩‍👧‍👦".repeat(101), "본문"), Arguments.of("제목", "👨‍👩‍👧‍👦".repeat(10001))
        );
    }

    @DisplayName("한글과 복합 이모지는 최대 표시 글자 수까지 공지를 생성할 수 있다")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("maximumNotices")
    void acceptMaximumDisplayLength(String title, String body) {
        // given
        // when & then
        assertThatCode(() -> new Notice(title, body)).doesNotThrowAnyException();
    }

    static Stream<Arguments> maximumNotices() {
        return Stream.of(
            Arguments.of("가".repeat(100), "나".repeat(10000)),
            Arguments.of("👨‍👩‍👧‍👦".repeat(100), "👨‍👩‍👧‍👦".repeat(10000))
        );
    }
}
