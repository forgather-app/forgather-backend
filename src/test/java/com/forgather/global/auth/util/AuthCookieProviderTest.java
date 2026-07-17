package com.forgather.global.auth.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.forgather.global.config.AuthCookieProperties;
import com.forgather.global.config.JwtProperties;

class AuthCookieProviderTest {

    private static final String SECRET =
        "masdfokosdfobluelunapongjumokohistaleopostymingobluesfsdfngobluelrere";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600L;
    private static final long REFRESH_TOKEN_EXPIRATION = 7200L;

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
        new JwtProperties(ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, SECRET)
    );

    @DisplayName("access token 쿠키는 API 전체 경로에 생성된다")
    @Test
    void createAccessTokenCookie() {
        // given
        AuthCookieProvider authCookieProvider = createAuthCookieProvider(false, "Lax");
        String accessToken = jwtTokenProvider.generateAccessToken(1L);

        // when
        ResponseCookie cookie = authCookieProvider.createAccessTokenCookie(accessToken);

        // then
        assertAll(
            () -> assertThat(cookie.getName()).isEqualTo(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME),
            () -> assertThat(cookie.getValue()).isEqualTo(accessToken),
            () -> assertThat(cookie.getPath()).isEqualTo("/"),
            () -> assertThat(cookie.isHttpOnly()).isTrue(),
            () -> assertThat(cookie.isSecure()).isFalse(),
            () -> assertThat(cookie.getSameSite()).isEqualTo("Lax"),
            () -> assertThat(cookie.getMaxAge()).isBetween(
                Duration.ofSeconds(ACCESS_TOKEN_EXPIRATION - 5),
                Duration.ofSeconds(ACCESS_TOKEN_EXPIRATION)
            )
        );
    }

    @DisplayName("refresh token 쿠키는 재발급 경로에 생성된다")
    @Test
    void createRefreshTokenCookieWithDevPolicy() {
        // given
        AuthCookieProvider authCookieProvider = createAuthCookieProvider(true, "None");
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        // when
        ResponseCookie cookie = authCookieProvider.createRefreshTokenCookie(refreshToken);

        // then
        assertAll(
            () -> assertThat(cookie.getName()).isEqualTo(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME),
            () -> assertThat(cookie.getValue()).isEqualTo(refreshToken),
            () -> assertThat(cookie.getPath()).isEqualTo("/auth/refresh"),
            () -> assertThat(cookie.isHttpOnly()).isTrue(),
            () -> assertThat(cookie.isSecure()).isTrue(),
            () -> assertThat(cookie.getSameSite()).isEqualTo("None"),
            () -> assertThat(cookie.getMaxAge()).isBetween(
                Duration.ofSeconds(REFRESH_TOKEN_EXPIRATION - 5),
                Duration.ofSeconds(REFRESH_TOKEN_EXPIRATION)
            )
        );
    }

    private AuthCookieProvider createAuthCookieProvider(boolean secure, String sameSite) {
        AuthCookieProperties authCookieProperties = new AuthCookieProperties(secure, sameSite);
        return new AuthCookieProvider(jwtTokenProvider, authCookieProperties);
    }
}
