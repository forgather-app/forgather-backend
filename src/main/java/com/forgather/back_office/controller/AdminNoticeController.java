package com.forgather.back_office.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.dto.AdminNoticeResponse;
import com.forgather.back_office.dto.NoticeDetailResponse;
import com.forgather.back_office.dto.RegisterNoticeRequest;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.service.AdminNoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @PostMapping
    public ResponseEntity<NoticeDetailResponse> create(
        @Valid @RequestBody RegisterNoticeRequest request,
        @Admin AdminUser adminUser
    ) {
        var response = adminNoticeService.create(request);
        return ResponseEntity.created(URI.create("/admin/notices/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<AdminNoticeResponse> getAllNotices(
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
        @Admin AdminUser adminUser
    ) {
        var response = adminNoticeService.getAllNotices(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNotice(
        @PathVariable Long noticeId,
        @Admin AdminUser adminUser
    ) {
        var response = adminNoticeService.getNotice(noticeId);
        return ResponseEntity.ok(response);
    }
}
