package com.forgather.domain.guestbook.dto;

import jakarta.validation.constraints.NotNull;

public record CreateGuestBookReportRequest(
    @NotNull
    Long reasonId,
    String detail
) {
}
