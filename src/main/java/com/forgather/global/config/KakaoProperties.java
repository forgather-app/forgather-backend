package com.forgather.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    @NotBlank
    private final String clientId;

    @NotBlank
    private final String jwksUrl;

    @NotBlank
    private final String adminKey;

    @NotBlank
    private final String unlinkUrl;
}
