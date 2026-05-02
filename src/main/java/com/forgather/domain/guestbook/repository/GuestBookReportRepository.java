package com.forgather.domain.guestbook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.global.auth.model.AppUser;

public interface GuestBookReportRepository {

    GuestBookReport save(GuestBookReport report);

    boolean existsByGuestBookCardAndReporterUser(GuestBookCard guestBookCard, AppUser reporterUser);

    Page<GuestBookReport> findAllByReporterUser(AppUser loginUser, Pageable pageable);
}
