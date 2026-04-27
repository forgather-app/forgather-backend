package com.forgather.domain.guestbook.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.forgather.domain.guestbook.dto.ReportHistoryDto;
import com.forgather.domain.guestbook.dto.ReportHistoryResponse;
import com.forgather.domain.guestbook.service.GuestBookReportService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Guestbook: 방명록", description = "방명록 관련 API")
@RequiredArgsConstructor
@RequestMapping("/guestbook")
@Controller
public class GuestbookController {

    private final GuestBookReportService guestBookReportService;

    @GetMapping("/me/reports")
    public ResponseEntity<ReportHistoryResponse> retrieveReportHistory(
        @LoginHost(required = true) Host loginUser,
        @PageableDefault(size = 15, sort = {"createdAt"}, direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        Page<ReportHistoryDto> reportHistory = guestBookReportService.retrieveReportHistory(loginUser, pageable);
        ReportHistoryResponse response = ReportHistoryResponse.from(reportHistory);
        return ResponseEntity.ok(response);
    }
}
