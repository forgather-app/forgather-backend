package com.forgather.global.auth.dto;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.forgather.global.exception.BaseException;

public record AppleUser(
    AppleUserName name,
    String email
) {

    public String fullName() {
        if (name == null || !StringUtils.hasText(name.lastName()) || !StringUtils.hasText(name.firstName())) {
            throw new BaseException("Apple user name is required", HttpStatus.BAD_REQUEST);
        }
        return name.lastName() + name.firstName();
    }

    public record AppleUserName(
        String firstName,
        String lastName
    ) {
    }
}
