package com.forgather.global.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "client-version")
public class ClientVersionProperties {

    private final boolean enabled;
    private final MinimumAppVersion minimumAppVersion;

    @Getter
    @RequiredArgsConstructor
    public static class MinimumAppVersion {

        private final String ios;
        private final String android;
    }
}
