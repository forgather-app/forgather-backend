package com.forgather.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplePropertiesTest {

    @DisplayName("문자열 audience가 허용 목록에 있으면 true를 반환한다")
    @Test
    void isAllowedAudienceWithString() {
        // given
        AppleProperties appleProperties = new AppleProperties(
            "https://appleid.apple.com/auth/keys",
            "https://appleid.apple.com",
            List.of("test-apple-audience")
        );

        // when
        boolean allowed = appleProperties.isAllowedAudience("test-apple-audience");

        // then
        assertThat(allowed).isTrue();
    }

    @DisplayName("배열 audience 중 허용 목록에 포함된 값이 있으면 true를 반환한다")
    @Test
    void isAllowedAudienceWithArray() {
        // given
        AppleProperties appleProperties = new AppleProperties(
            "https://appleid.apple.com/auth/keys",
            "https://appleid.apple.com",
            List.of("test-apple-audience")
        );

        // when
        boolean allowed = appleProperties.isAllowedAudience(List.of("other-audience", "test-apple-audience"));

        // then
        assertThat(allowed).isTrue();
    }
}
