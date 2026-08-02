package com.forgather.domain.guestbook.repository.dto;

import java.time.LocalDateTime;

public record GuestBookCardListDto(
    Long id,
    String nickname,
    String message,
    LocalDateTime createdAt,
    boolean isRead,
    boolean isPhotoExists
) {
}
