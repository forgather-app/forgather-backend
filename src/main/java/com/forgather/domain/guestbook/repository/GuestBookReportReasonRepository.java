package com.forgather.domain.guestbook.repository;

import java.util.Optional;

import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface GuestBookReportReasonRepository {

    Optional<GuestBookReportReason> findByIdAndIsHiddenFalse(Long id);

    default GuestBookReportReason getByIdAndIsHiddenFalseOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("신고 사유의 id는 null일 수 없습니다.");
        }
        return findByIdAndIsHiddenFalse(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 신고 사유입니다. id: " + id));
    }
}
