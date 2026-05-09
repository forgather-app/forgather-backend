package com.forgather.domain.guestbook.dto;

import java.util.List;

import com.forgather.domain.guestbook.model.GuestBookReportReason;

public record ReportReasonsResponse(
    List<ReasonInfo> reasons
) {

    public static ReportReasonsResponse from(List<GuestBookReportReason> reasons) {
        return new ReportReasonsResponse(reasons.stream()
            .map(reason -> new ReasonInfo(reason.getId(), reason.getLabel()))
            .toList());
    }

    public record ReasonInfo(Long id, String label) {
    }
}
