package com.forgather.global.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "apple")
public class AppleProperties {

    private final String jwksUrl;
    private final String issuer;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKey;
    private final String tokenUrl;

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
