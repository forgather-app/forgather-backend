package com.forgather.domain.guestbook.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.guestbook.dto.ReportHistoryDto;
import com.forgather.domain.guestbook.dto.ReportHistoryResponse;
import com.forgather.domain.guestbook.service.GuestBookReportService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Guestbook: 방명록", description = "방명록 관련 API")
@RequiredArgsConstructor
@RequestMapping("/guestbook")
@RestController
public class GuestbookController {

    private final GuestBookReportService guestBookReportService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 신고내역 조회",
        description = "로그인 사용자의 방명록 신고 내역 조회",
        parameters = {
            @Parameter(
                name = "page",
                description = "페이지 번호 (1부터 시작)",
                schema = @Schema(type = "integer", minimum = "1"),
                example = "1"
            ),
            @Parameter(
                name = "size",
                description = "페이지 크기",
                schema = @Schema(type = "integer", minimum = "1"),
                example = "15"
            ),
            @Parameter(
                name = "sort",
                description = "정렬 조건 (여러개 지정 가능) (id,desc 포함 필수)",
                example = "createdAt,desc",
                array = @ArraySchema(schema = @Schema(type = "string"))
            )
        }
    )
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
