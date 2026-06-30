package com.forgather.domain.term.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.service.TermService;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Term: 약관", description = "약관 조회 API")
@RequiredArgsConstructor
@RequestMapping("/terms")
@RestController
public class TermController {

    private final TermService termService;

    @Operation(summary = "최신 약관 목록 조회", description = "비로그인 상태로 온보딩에 필요한 최신 약관 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TermResponse>>> getLatestTerms() {
        var response = termService.getLatestTerms();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
