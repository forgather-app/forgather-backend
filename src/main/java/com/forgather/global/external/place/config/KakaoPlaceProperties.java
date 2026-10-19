package com.forgather.global.external.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoPlaceProperties(
    @NotBlank String restApiKey,

    @NotBlank @DefaultValue("https://dapi.kakao.com/v2/local/search/keyword.json") String placeSearchUrl
) {
}
