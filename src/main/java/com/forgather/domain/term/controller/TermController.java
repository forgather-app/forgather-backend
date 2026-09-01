package com.forgather.domain.term.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.annotation.LoginHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.term.dto.TermAgreementResponse;
import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.service.TermService;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Term: 약관", description = "약관 조회 및 동의 API")
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

    @Operation(summary = "내 약관 동의 현황 조회",
        description = "로그인한 호스트의 약관별 동의 상태를 조회합니다. "
            + "약관 정보는 항상 타입별 최신 약관 기준이며 노출 순서(sortOrder) 오름차순으로 반환합니다. "
            + "약관이 실질적으로 개정되어 기존 동의가 무효화되면 isReagreementRequired가 true가 됩니다. "
            + "로그인된 사용자가 없으면 401 Unauthorized를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TermAgreementResponse>>> getMyTermAgreements(@LoginHost Host host) {
        var response = termService.getMyTermAgreements(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "약관 동의",
        description = "로그인한 호스트가 특정 약관에 동의합니다. 필수·선택 약관 모두 동의할 수 있고, "
            + "개정으로 무효화된 동의의 재동의도 이 API로 처리합니다. "
            + "이미 동의한 상태면 이력을 추가하지 않고 현재 상태를 그대로 반환합니다. "
            + "타입별 최신 약관이 아니거나 온보딩을 마치지 않은 호스트면 400 Bad Request, "
            + "존재하지 않거나 삭제된 약관이면 404 Not Found, "
            + "로그인된 사용자가 없으면 401 Unauthorized를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{termId}/agreement")
    public ResponseEntity<ApiResponse<TermAgreementResponse>> agreeTerm(
        @LoginHost Host host,
        @PathVariable Long termId
    ) {
        var response = termService.agreeTerm(host, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "약관 동의 철회",
        description = "로그인한 호스트가 특정 약관의 동의를 철회합니다. 선택 약관만 철회할 수 있습니다. "
            + "이미 미동의 상태면 이력을 추가하지 않고 현재 상태를 그대로 반환합니다. "
            + "필수 약관이거나 타입별 최신 약관이 아니거나 온보딩을 마치지 않은 호스트면 400 Bad Request, "
            + "존재하지 않거나 삭제된 약관이면 404 Not Found, "
            + "로그인된 사용자가 없으면 401 Unauthorized를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{termId}/agreement")
    public ResponseEntity<ApiResponse<TermAgreementResponse>> withdrawTerm(
        @LoginHost Host host,
        @PathVariable Long termId
    ) {
        var response = termService.withdrawTerm(host, termId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
