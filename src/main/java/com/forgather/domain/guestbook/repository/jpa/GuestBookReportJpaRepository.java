package com.forgather.domain.guestbook.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.domain.guestbook.repository.GuestBookReportRepository;

public interface GuestBookReportJpaRepository
    extends JpaRepository<GuestBookReport, Long>, GuestBookReportRepository {
}
