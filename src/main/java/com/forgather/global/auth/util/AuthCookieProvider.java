package com.forgather.global.auth.util;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.forgather.global.config.AuthCookieProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private static final String ACCESS_TOKEN_COOKIE_PATH = "/";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/auth/refresh";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieProperties authCookieProperties;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return createCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_COOKIE_PATH);
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return createCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, REFRESH_TOKEN_COOKIE_PATH);
    }

    private ResponseCookie createCookie(String name, String token, String path) {
        long maxAge = jwtTokenProvider.getRemainingExpirationSeconds(token);
        return ResponseCookie.from(name, token)
            .httpOnly(true)
            .secure(authCookieProperties.isSecure())
            .sameSite(authCookieProperties.getSameSite())
            .path(path)
            .maxAge(Duration.ofSeconds(maxAge))
            .build();
    }
}
