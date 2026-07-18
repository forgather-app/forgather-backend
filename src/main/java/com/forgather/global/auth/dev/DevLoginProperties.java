package com.forgather.global.auth.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Profile({"local", "dev", "test"})
@ConfigurationProperties(prefix = "auth.dev-login")
public class DevLoginProperties {

    private final String loginId;
    private final String password;
    private final String nickname;
}
