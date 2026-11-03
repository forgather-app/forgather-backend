package com.forgather.domain.export.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import com.forgather.global.exception.InvalidConfigurationException;

@ConfigurationProperties(prefix = "space.pdf-export")
public record SpacePdfExportProperties(
    String functionName,
    String region,
    String publicUrlBase
) {

    public void validateConfigured() {
        if (!StringUtils.hasText(functionName) || !StringUtils.hasText(region)
            || !StringUtils.hasText(publicUrlBase)) {
            throw unavailable();
        }
        try {
            URI base = URI.create(publicUrlBase);
            if (!("https".equals(base.getScheme()) || "http".equals(base.getScheme()))
                || base.getHost() == null || base.getRawQuery() != null || base.getRawFragment() != null
                || base.getUserInfo() != null) {
                throw unavailable();
            }
        } catch (IllegalArgumentException e) {
            throw unavailable();
        }
    }

    public String publicUrl(String path) {
        String base = publicUrlBase.replaceAll("/+$", "");
        return base + "/" + UriUtils.encodePath(path.replaceFirst("^/+", ""), StandardCharsets.UTF_8);
    }

    private InvalidConfigurationException unavailable() {
        return new InvalidConfigurationException(
            "space.pdf-export.function-name, space.pdf-export.region, space.pdf-export.public-url-base");
    }
}
