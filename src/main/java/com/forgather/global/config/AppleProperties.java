package com.forgather.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.forgather.global.util.AudienceMatcher;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "apple")
public class AppleProperties {

    @NotBlank
    private final String jwksUrl;

    @NotBlank
    private final String issuer;

    @NotBlank
    private final String clientId;

    @NotBlank
    private final String teamId;

    @NotBlank
    private final String keyId;

    @NotBlank
    private final String privateKey;

    @NotBlank
    private final String tokenUrl;

    @NotBlank
    private final String revokeUrl;

    public boolean isAllowedAudience(Object audience) {
        return AudienceMatcher.matches(clientId, audience);
    }
}
