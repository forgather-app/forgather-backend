package com.forgather.global.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
        if (clientId == null || audience == null) {
            return false;
        }
        if (audience instanceof String value) {
            return clientId.equals(value);
        }
        if (audience instanceof Collection<?> values) {
            return values.stream().anyMatch(clientId::equals);
        }
        if (audience instanceof Object[] values) {
            return Arrays.stream(values).anyMatch(clientId::equals);
        }
        return false;
    }
}
