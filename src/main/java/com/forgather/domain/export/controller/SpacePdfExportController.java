package com.forgather.domain.export.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.export.dto.ExportSpacePdfRequest;
import com.forgather.domain.export.dto.ExportSpacePdfResponse;
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
    @Operation(summary = "스페이스 PDF 추출 요청", description = "스페이스 호스트가 PDF 추출 및 이메일 발송 작업을 요청합니다.")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<ApiResponse<ExportSpacePdfResponse>> request(
        @PathVariable String spaceCode,
        @LoginHost Host host,
        @RequestBody @Valid ExportSpacePdfRequest request
    ) {
        var response = service.request(spaceCode, host, request);
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }
}
