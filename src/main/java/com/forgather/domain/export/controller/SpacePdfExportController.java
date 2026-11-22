package com.forgather.domain.export.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.export.dto.ExportSpacePdfRequest;
import com.forgather.domain.export.dto.ExportSpacePdfResponse;
import com.forgather.domain.export.dto.SpacePdfExportUsageResponse;
import com.forgather.domain.export.service.SpacePdfExportService;
import com.forgather.domain.host.model.Host;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Space: 스페이스")
public class SpacePdfExportController {

    private final SpacePdfExportService service;

    @PostMapping("/spaces/{spaceCode}/exports/pdf")
    @Operation(summary = "스페이스 PDF 추출 요청",
        description = "스페이스 호스트가 PDF 추출 및 이메일 발송 작업을 요청합니다. 한국 시간 자정 기준 사용자당 하루 5회까지 접수하며, 한도 도달 시 400을 반환합니다.")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<ApiResponse<ExportSpacePdfResponse>> request(
        @PathVariable String spaceCode,
        @LoginHost Host host,
        @RequestBody @Valid ExportSpacePdfRequest request
    ) {
        var response = service.request(spaceCode, host, request);
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }

    @GetMapping("/hosts/me/exports/pdf/usage")
    @Operation(summary = "오늘 PDF 추출 사용 횟수 조회",
        description = "로그인한 사용자의 한국 시간 기준 오늘 접수 횟수와 하루 한도를 반환합니다. 접수 이후 처리에 실패한 요청도 포함합니다.")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<ApiResponse<SpacePdfExportUsageResponse>> getUsage(@LoginHost Host host) {
        var response = service.getUsage(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
