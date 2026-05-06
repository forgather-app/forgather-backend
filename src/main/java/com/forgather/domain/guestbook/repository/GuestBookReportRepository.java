package com.forgather.domain.guestbook.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface GuestBookReportRepository {

    GuestBookReport save(GuestBookReport report);

    boolean existsByGuestBookCardAndReporter(GuestBookCard guestBookCard, Host reporter);

    Page<GuestBookReport> findAllByReporter(Host loginUser, Pageable pageable);

    Optional<GuestBookReport> findById(Long id);

    default GuestBookReport getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("신고 내역 id는 null일 수 없습니다.");
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 방명록 신고 내역입니다. reportId: " + id));
    }
}
