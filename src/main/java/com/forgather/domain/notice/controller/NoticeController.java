package com.forgather.domain.notice.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.notice.dto.NoticeDetailResponse;
import com.forgather.domain.notice.dto.NoticesResponse;
import com.forgather.domain.notice.service.NoticeService;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notice: 공지", description = "로그인 없이 공지를 조회하는 공개 API")
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록 조회", description = "로그인 없이 공지를 조회합니다. 페이지는 1부터 시작하며 "
        + "요청 크기 및 정렬과 관계없이 생성일과 id 내림차순으로 15개씩 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NoticesResponse>> getAllNotices(
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        NoticesResponse response = noticeService.getAllNotices(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지 상세 조회", description = "로그인 없이 공지의 제목, 원문 본문, 최초 작성일을 조회합니다.")
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(@PathVariable Long noticeId) {
        NoticeDetailResponse response = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
