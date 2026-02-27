package com.forgather.back_office.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(
    String prometheusUrl,
    String dashboardUrl
) {
}
