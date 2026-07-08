package com.forgather.global.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "apple")
public class AppleProperties {

    private final String jwksUrl;
    private final String issuer;
    private final List<String> allowedAudiences;

    public boolean isAllowedAudience(Object audience) {
        if (audience == null || allowedAudiences == null) {
            return false;
        }
        if (audience instanceof String value) {
            return allowedAudiences.contains(value);
        }
        if (audience instanceof Collection<?> values) {
            return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(allowedAudiences::contains);
        }
        if (audience instanceof Object[] values) {
            return Arrays.stream(values)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(allowedAudiences::contains);
        }
        return false;
    }
}
