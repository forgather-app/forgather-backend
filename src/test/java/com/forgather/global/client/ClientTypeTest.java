package com.forgather.global.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientTypeTest {

    @DisplayName("대소문자와 관계없이 클라이언트 타입을 찾는다.")
    @ParameterizedTest
    @CsvSource({"WEB, WEB", "web, WEB", "IOS, IOS", "ios, IOS", "ANDROID, ANDROID", "Android, ANDROID"})
    void from(String value, ClientType expected) {
        assertThat(ClientType.from(value)).contains(expected);
    }

    @DisplayName("헤더가 없거나 알 수 없는 값이면 클라이언트 타입을 찾지 않는다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"APP", "DESKTOP"})
    void fromUnknown(String value) {
        assertThat(ClientType.from(value)).isEmpty();
    }
}
