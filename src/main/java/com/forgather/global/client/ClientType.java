package com.forgather.global.client;

import java.util.Arrays;
import java.util.Optional;

public enum ClientType {
    WEB,
    IOS,
    ANDROID,
    ;

    public static Optional<ClientType> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(value))
            .findFirst();
    }
}
