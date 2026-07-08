package com.forgather.global.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AppleIdToken(
    String iss,
    String aud,
    String sub,
    String email,
    Boolean emailVerified,
    Long exp,
    Long iat,
    String nonce
) {

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified)
            || "true".equalsIgnoreCase(String.valueOf(emailVerified));
    }
}
