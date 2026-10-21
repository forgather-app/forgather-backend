package com.forgather.global.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppVersionTest {

    @DisplayName("major.minor.patch 형식의 버전을 파싱한다.")
    @Test
    void parse() {
        assertThat(AppVersion.parse("1.10.3")).contains(new AppVersion(1, 10, 3));
    }

    @DisplayName("형식이 올바르지 않은 버전은 파싱하지 않는다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "1.2", "1.2.3.4", "-1.0.0", "1.0.0-beta", " 1.0.0", "v1.0.0", "99999999999.0.0"})
    void parseInvalidFormat(String value) {
        assertThat(AppVersion.parse(value)).isEmpty();
    }

    @DisplayName("버전은 문자열이 아닌 숫자 단위로 비교한다.")
    @Test
    void compareNumerically() {
        AppVersion version = new AppVersion(1, 10, 0);

        assertThat(version.isLowerThan(new AppVersion(1, 9, 0))).isFalse();
        assertThat(new AppVersion(1, 9, 0).isLowerThan(version)).isTrue();
    }

    @DisplayName("major, minor, patch 순서로 비교한다.")
    @Test
    void compareInOrder() {
        AppVersion version = new AppVersion(2, 0, 0);

        assertThat(new AppVersion(1, 99, 99).isLowerThan(version)).isTrue();
        assertThat(new AppVersion(2, 0, 1).isLowerThan(version)).isFalse();
    }

    @DisplayName("같은 버전은 낮은 버전이 아니다.")
    @Test
    void sameVersionIsNotLower() {
        assertThat(new AppVersion(1, 2, 0).isLowerThan(new AppVersion(1, 2, 0))).isFalse();
    }
}
