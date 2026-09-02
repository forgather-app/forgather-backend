package com.forgather.global.external.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.forgather.global.util.AudienceMatcher;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    /**
     * 네이티브 SDK가 토큰을 발급받을 때 client_id로 사용하는 앱 키.
     * id_token의 aud 클레임에 이 값이 담기므로 audience 검증 기준이 된다.
     */
    @NotBlank
    private final String nativeAppKey;

    @NotBlank
    private final String issuer;

    @NotBlank
    private final String jwksUrl;

    @NotBlank
    private final String adminKey;

    @NotBlank
    private final String unlinkUrl;

    public boolean isAllowedAudience(Object audience) {
        return AudienceMatcher.matches(nativeAppKey, audience);
    }
}
