package com.forgather.domain.guestbook.dto;

import java.time.LocalDateTime;

import com.forgather.domain.guestbook.model.GuestBookReport;

public record CreateGuestBookReportResponse(
    Long id,
    Long guestBookCardId,
    LocalDateTime createdAt
) {
    public static CreateGuestBookReportResponse from(GuestBookReport report) {
        return new CreateGuestBookReportResponse(
            report.getId(),
            report.getGuestBookCard().getId(),
            report.getCreatedAt()
        );
    }
}
