package com.forgather.fixture;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.model.ReporterType;
import com.forgather.global.auth.model.AppUser;

public class GuestBookReportFixture {

    public static GuestBookReport createReport(GuestBookCard card, AppUser user, GuestBookReportReason reason) {
        return new GuestBookReport(card, user, user, ReporterType.HOST, reason, null);
    }

    public static GuestBookReport createReportWithDetail(
        GuestBookCard card, AppUser user, GuestBookReportReason reason, String detail
    ) {
        return new GuestBookReport(card, user, user, ReporterType.HOST, reason, detail);
    }
}
