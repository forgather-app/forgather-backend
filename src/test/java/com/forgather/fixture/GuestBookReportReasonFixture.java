package com.forgather.fixture;

import com.forgather.domain.guestbook.model.GuestBookReportReason;

public class GuestBookReportReasonFixture {

    public static GuestBookReportReason createReason() {
        return new GuestBookReportReason("SPAM", "스팸", 1, false);
    }

    public static GuestBookReportReason createHiddenReason() {
        return new GuestBookReportReason("OTHER", "기타", 1, true);
    }

    public static GuestBookReportReason createReasonWithCode(String code, String label, int displayOrder) {
        return new GuestBookReportReason(code, label, displayOrder, false);
    }
}
