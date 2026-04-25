package com.forgather.domain.guestbook.dto;

public record CreateGuestBookReportRequest(
    Long reasonId,
    String detail
) {
}
