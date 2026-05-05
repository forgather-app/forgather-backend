package com.forgather.domain.guestbook.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface GuestBookReportRepository {

    GuestBookReport save(GuestBookReport report);

    boolean existsByGuestBookCardAndReporter(GuestBookCard guestBookCard, Host reporter);

    Page<GuestBookReport> findAllByReporter(Host loginUser, Pageable pageable);

    @Query("""
        SELECT r FROM GuestBookReport r
        JOIN FETCH r.guestBookCard c
        JOIN FETCH c.space
        JOIN FETCH r.reason
        WHERE r.id = :id AND r.reporter = :reporter
        """)
    Optional<GuestBookReport> findByIdWithDetailsAndReporter(
        @Param("id") Long id,
        @Param("reporter") Host reporter
    );

    default GuestBookReport getByIdAndReporterOrThrow(Long id, Host reporter) {
        if (id == null) {
            throw new BaseNullPointerException("신고 id는 null일 수 없습니다.");
        }
        if (reporter == null) {
            throw new BaseNullPointerException("신고자는 null일 수 없습니다.");
        }
        return findByIdWithDetailsAndReporter(id, reporter)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 신고이거나 조회 권한이 없습니다. reportId: " + id));
    }
}
