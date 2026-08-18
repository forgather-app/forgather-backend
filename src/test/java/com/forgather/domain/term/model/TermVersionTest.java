package com.forgather.domain.term.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class TermVersionTest {

    @DisplayName("semver 형식 문자열로 생성한다")
    @Test
    void createWithSemverString() {
        // when
        TermVersion version = new TermVersion("2.10.3");

        // then
        assertThat(version.getValue()).isEqualTo("2.10.3");
    }

    @DisplayName("같은 버전끼리는 동등하다")
    @Test
    void compareSameVersion() {
        // when & then
        assertAll(
            () -> assertThat(new TermVersion("1.0.0")).isEqualTo(new TermVersion("1.0.0")),
            () -> assertThat(new TermVersion("1.0.0")).isEqualByComparingTo(new TermVersion("1.0.0")),
            () -> assertThat(new TermVersion("1.0.0").isAtLeast(new TermVersion("1.0.0"))).isTrue()
        );
    }

    @DisplayName("자릿수가 달라도 문자열이 아닌 정수로 비교한다")
    @Test
    void compareByNumberNotByString() {
        // when & then
        assertAll(
            () -> assertThat(new TermVersion("10.0.0")).isGreaterThan(new TermVersion("2.0.0")),
            () -> assertThat(new TermVersion("1.2.0")).isGreaterThan(new TermVersion("1.1.9")),
            () -> assertThat(new TermVersion("1.0.9")).isLessThan(new TermVersion("1.0.10"))
        );
    }

    @DisplayName("작은 버전은 최소 요구 버전을 만족하지 못한다")
    @Test
    void isAtLeastReturnsFalseWhenLower() {
        // when & then
        assertAll(
            () -> assertThat(new TermVersion("1.0.0").isAtLeast(new TermVersion("2.0.0"))).isFalse(),
            () -> assertThat(new TermVersion("2.0.0").isAtLeast(new TermVersion("1.0.0"))).isTrue()
        );
    }

    @DisplayName("semver 형식이 아니면 생성할 수 없다")
    @ParameterizedTest
    @ValueSource(strings = {
        "1", "1.0", "1.0.0.0", "v1.0.0", "1.0.a", "", " 1.0.0", "1.0.0 ",
        "1234567890.0.0", "01.0.0", "1.00.0", "1.0.00"
    })
    void throwExceptionWhenFormatIsInvalid(String version) {
        // when & then
        assertThatThrownBy(() -> new TermVersion(version))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("약관 버전 형식이 올바르지 않습니다.");
    }

    @DisplayName("버전이 null이면 생성할 수 없다")
    @Test
    void throwExceptionWhenVersionIsNull() {
        // when & then
        assertThatThrownBy(() -> new TermVersion(null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("약관 버전은 null일 수 없습니다.");
    }
}
