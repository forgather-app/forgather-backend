package com.forgather.back_office.dto;

public record AdminLoginRequest(
    String username,
    String password
) {

    @Override
    public String toString() {
        return "AdminLoginRequest[username=" + username + ", password=***]";
    }
}
