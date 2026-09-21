package com.forgather.domain.guestbook.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.space.model.Space;

public record ReportDetailResponse(
    Long id,
    SpaceInfo space,
    String reason,
    String detail,
    String nicknameSnapshot,
    String messageSnapshot,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAtSnapshot,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {

    public record SpaceInfo(String spaceCode, String name) {
    }

    public static ReportDetailResponse from(GuestBookReport report) {
        Space space = report.getGuestBookCard().getSpace();
        GuestBookReportReason reason = report.getReason();
        return new ReportDetailResponse(
            report.getId(),
            new SpaceInfo(space.getCode(), space.getName()),
            reason.getLabel(),
            report.getDetail(),
            report.getNicknameSnapshot(),
            report.getMessageSnapshot(),
            report.getCreatedAtSnapshot(),
            report.getCreatedAt()
        );
    }
}
