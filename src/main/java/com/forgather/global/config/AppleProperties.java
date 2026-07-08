package com.forgather.global.config;

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

    public boolean isAllowedAudience(String audience) {
        return audience != null
            && allowedAudiences != null
            && allowedAudiences.contains(audience);
    }
}
