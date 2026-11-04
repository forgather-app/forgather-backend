package com.forgather.domain.guestbook.repository.dto;

import java.time.LocalDateTime;

public record GuestBookArchiveCardDto(
    Long id,
    String nickname,
    String message,
    LocalDateTime createdAt
) {
}
