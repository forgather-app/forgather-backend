package com.forgather.domain.notice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.notice.dto.NoticeDetailResponse;
import com.forgather.domain.notice.dto.NoticesResponse;
import com.forgather.domain.notice.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticesResponse getAllNotices(Pageable pageable) {
        PageRequest fixedPage = PageRequest.of(pageable.getPageNumber(), 15,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return NoticesResponse.from(noticeRepository.findAllByDeletedAtIsNull(fixedPage));
    }

    public NoticeDetailResponse getNotice(Long noticeId) {
        return NoticeDetailResponse.from(noticeRepository.getByIdAndDeletedAtIsNullOrThrow(noticeId));
    }
}
