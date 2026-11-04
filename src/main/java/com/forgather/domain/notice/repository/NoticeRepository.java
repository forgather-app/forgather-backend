package com.forgather.domain.notice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forgather.domain.notice.model.Notice;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface NoticeRepository {

    Notice save(Notice notice);

    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);

    Page<Notice> findAllByDeletedAtIsNull(Pageable pageable);

    default Notice getByIdAndDeletedAtIsNullOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("공지의 id는 null일 수 없습니다. id: " + id);
        }
        return findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 공지입니다. id: " + id));
    }
}
