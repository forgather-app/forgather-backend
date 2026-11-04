package com.forgather.domain.export.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import com.forgather.global.exception.InvalidConfigurationException;

@ConfigurationProperties(prefix = "space.pdf-export")
public record SpacePdfExportProperties(
    String functionName,
    String region
) {

    public void validateConfigured() {
        if (!StringUtils.hasText(functionName) || !StringUtils.hasText(region)) {
            throw new InvalidConfigurationException("space.pdf-export.function-name, space.pdf-export.region");
        }
    }
}
