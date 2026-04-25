package com.forgather.domain.guestbook.repository;

import java.util.Optional;

import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.NotFoundException;

public interface GuestBookReportReasonRepository {

    Optional<GuestBookReportReason> findById(Long id);

    default GuestBookReportReason getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseException("신고 사유의 id는 null일 수 없습니다.");
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 신고 사유입니다. id: " + id));
    }
}
