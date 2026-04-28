package com.forgather.domain.guestbook.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.repository.GuestBookReportReasonRepository;

public interface GuestBookReportReasonJpaRepository
    extends JpaRepository<GuestBookReportReason, Long>, GuestBookReportReasonRepository {
}
