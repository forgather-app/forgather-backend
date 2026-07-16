package com.forgather.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private final boolean secure;
    private final String sameSite;
}
