package com.forgather.fixture;

import com.forgather.domain.guestbook.model.GuestBookReportReason;

public class GuestBookReportReasonFixture {

    public static GuestBookReportReason createReason() {
        return new GuestBookReportReason("SPAM", "스팸", 1, false);
    }
}
