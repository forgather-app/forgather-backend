package com.forgather.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplePropertiesTest {

    @DisplayName("ID token audience가 Apple client ID와 같으면 허용한다")
    @Test
    void isAllowedAudienceWithString() {
        // given
        AppleProperties appleProperties = appleProperties();

        // when
        boolean allowed = appleProperties.isAllowedAudience("test-apple-audience");

        // then
        assertThat(allowed).isTrue();
    }

    @DisplayName("ID token audience가 Apple client ID와 다르면 허용하지 않는다")
    @Test
    void isAllowedAudienceWithDifferentClientId() {
        // given
        AppleProperties appleProperties = appleProperties();

        // when
        boolean allowed = appleProperties.isAllowedAudience("other-audience");

        // then
        assertThat(allowed).isFalse();
    }

    private AppleProperties appleProperties() {
        return new AppleProperties(
            "https://appleid.apple.com/auth/keys",
            "https://appleid.apple.com",
            "test-apple-audience",
            "test-team-id",
            "test-key-id",
            "test-private-key",
            "https://appleid.apple.com/auth/token"
        );
    }
}
