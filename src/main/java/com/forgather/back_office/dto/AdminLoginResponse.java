package com.forgather.back_office.dto;

public record AdminLoginResponse(
    String sessionId
) {

    public static AdminLoginResponse of(String sessionId) {
        return new AdminLoginResponse(sessionId);
    }
}
