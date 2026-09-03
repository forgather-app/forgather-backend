package com.forgather.fixture;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.model.ReporterType;
import com.forgather.domain.host.model.Host;

public class GuestBookReportFixture {

    public static GuestBookReport createReport(GuestBookCard card, Host host, GuestBookReportReason reason) {
        return new GuestBookReport(card, host, host, ReporterType.HOST, reason, null);
    }

    public static GuestBookReport createReportWithDetail(
        GuestBookCard card, Host host, GuestBookReportReason reason, String detail
    ) {
        return new GuestBookReport(card, host, host, ReporterType.HOST, reason, detail);
    }
}
