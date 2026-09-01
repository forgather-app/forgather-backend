package com.forgather.domain.host.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.dto.HostResponse;
import com.forgather.domain.host.dto.OnboardingRequest;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.service.HostAccountService;
import com.forgather.domain.host.service.WithdrawService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Host: 회원", description = "회원 계정 API")
public class HostAccountController {

    private final HostAccountService hostAccountService;
    private final WithdrawService withdrawService;
    private final AuthCookieProvider authCookieProvider;

    @PostMapping("/onboarding")
    @Operation(summary = "온보딩 완료",
        description = "서비스 닉네임과 약관 동의 이력을 함께 저장합니다. " +
            "이미 온보딩이 완료된 호스트가 다시 호출하면 409 Conflict를 반환합니다.")
    public ResponseEntity<ApiResponse<HostResponse>> submitOnboarding(
        @LoginHost Host host,
        @RequestBody OnboardingRequest request
    ) {
        var response = hostAccountService.submitOnboarding(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴",
        description = "회원을 탈퇴 처리합니다. 소셜 연결(Kakao/Apple)을 해제하고 계정과 소유 콘텐츠를 삭제합니다. " +
            "성공 시 액세스토큰과 리프레시토큰 쿠키를 만료시킵니다. " +
            "탈퇴 후 같은 소셜 계정으로 다시 로그인하면 신규 가입으로 처리됩니다.")
    public ResponseEntity<ApiResponse<Void>> withdraw(@LoginHost Host host) {
        withdrawService.withdraw(host);
        return createExpiredCookieResponse();
    }

    private ResponseEntity<ApiResponse<Void>> createExpiredCookieResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieProvider.expireAccessTokenCookie().toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieProvider.expireRefreshTokenCookie().toString());
        return ResponseEntity.ok()
            .headers(headers)
            .body(ApiResponse.success());
    }
}
