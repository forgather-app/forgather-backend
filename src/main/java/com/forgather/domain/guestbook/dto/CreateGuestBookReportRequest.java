package com.forgather.domain.guestbook.dto;

import com.forgather.domain.guestbook.model.GuestBookReportReason;

import jakarta.validation.constraints.NotNull;

public record CreateGuestBookReportRequest(
    @NotNull
    GuestBookReportReason reason,
    String detail
) {
}
