package com.forgather.global.external.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private final String jwksUrl;
}
