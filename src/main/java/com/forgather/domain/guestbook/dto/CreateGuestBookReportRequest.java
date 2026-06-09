package com.forgather.domain.guestbook.dto;

import com.forgather.global.validation.TextSize;

import jakarta.validation.constraints.NotNull;

public record CreateGuestBookReportRequest(
    @NotNull
    Long reasonId,

    @TextSize(min = 5, max = 200)
    String detail
) {
}
