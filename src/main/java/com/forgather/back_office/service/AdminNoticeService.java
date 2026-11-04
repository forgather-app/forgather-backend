package com.forgather.back_office.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminNoticeResponse;
import com.forgather.back_office.dto.NoticeDetailResponse;
import com.forgather.back_office.dto.RegisterNoticeRequest;
import com.forgather.domain.notice.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeDetailResponse create(RegisterNoticeRequest request) {
        return NoticeDetailResponse.from(noticeRepository.save(request.toEntity()));
    }

    public AdminNoticeResponse getAllNotices(Pageable pageable) {
        PageRequest fixedPage = PageRequest.of(pageable.getPageNumber(), 15,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return AdminNoticeResponse.from(noticeRepository.findAllByDeletedAtIsNull(fixedPage));
    }

    public NoticeDetailResponse getNotice(Long noticeId) {
        return NoticeDetailResponse.from(noticeRepository.getByIdAndDeletedAtIsNullOrThrow(noticeId));
    }
}
