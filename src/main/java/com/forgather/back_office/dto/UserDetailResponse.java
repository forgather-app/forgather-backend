package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.global.auth.model.AppUser;

public record UserDetailResponse(
    Long id,
    String name,
    LocalDateTime createdAt,
    long spaceCount
) {

    public static UserDetailResponse of(AppUser user, long spaceCount) {
        return new UserDetailResponse(user.getId(), user.getName(), user.getCreatedAt(), spaceCount);
    }
}
