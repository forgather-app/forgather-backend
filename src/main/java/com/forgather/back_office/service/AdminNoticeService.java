package com.forgather.back_office.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminNoticeResponse;
import com.forgather.back_office.dto.AdminNoticeDetailResponse;
import com.forgather.back_office.dto.RegisterNoticeRequest;
import com.forgather.back_office.dto.UpdateNoticeRequest;
import com.forgather.domain.notice.model.Notice;
import com.forgather.domain.notice.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public AdminNoticeDetailResponse create(RegisterNoticeRequest request) {
        return AdminNoticeDetailResponse.from(noticeRepository.save(request.toEntity()));
    }

    @Transactional
    public AdminNoticeDetailResponse update(Long noticeId, UpdateNoticeRequest request) {
        Notice notice = noticeRepository.getByIdAndDeletedAtIsNullOrThrow(noticeId);
        notice.update(request.title(), request.body());
        return AdminNoticeDetailResponse.from(notice);
    }

    @Transactional
    public void delete(Long noticeId) {
        Notice notice = noticeRepository.getByIdAndDeletedAtIsNullOrThrow(noticeId);
        notice.delete();
    }

    public AdminNoticeResponse getAllNotices(Pageable pageable) {
        PageRequest fixedPage = PageRequest.of(pageable.getPageNumber(), 15,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return AdminNoticeResponse.from(noticeRepository.findAllByDeletedAtIsNull(fixedPage));
    }

    public AdminNoticeDetailResponse getNotice(Long noticeId) {
        return AdminNoticeDetailResponse.from(noticeRepository.getByIdAndDeletedAtIsNullOrThrow(noticeId));
    }
}
