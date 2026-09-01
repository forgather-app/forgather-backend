package com.forgather.domain.host.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.HostResponse;
import com.forgather.domain.host.dto.OnboardingRequest;
import com.forgather.domain.host.dto.PublicHostProfileResponse;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.service.HostService;
import com.forgather.domain.host.service.WithdrawService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Host: 호스트", description = "호스트 관련 API")
public class HostController {

    private final HostService hostService;
    private final WithdrawService withdrawService;
    private final AuthCookieProvider authCookieProvider;

    @GetMapping("/hosts/me/profile")
    @Operation(summary = "내 프로필 조회",
        description = "로그인한 호스트의 프로필(닉네임, 한 줄 소개, 링크, 프로필 사진)을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HostProfileResponse>> getProfile(@LoginHost Host host) {
        var response = hostService.getProfile(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/hosts/me/profile")
    @Operation(summary = "내 프로필 수정",
        description = "로그인한 호스트의 프로필을 수정합니다. "
            + "null인 필드는 변경하지 않고, 빈 문자열은 값을 제거합니다. (닉네임은 빈 문자열 불가)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HostProfileResponse>> updateProfile(
        @LoginHost Host host,
        @Valid @RequestBody UpdateHostProfileRequest request
    ) {
        var response = hostService.updateProfile(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/hosts/{hostCode}/profile")
    @Operation(summary = "호스트 공개 프로필 조회",
        description = "호스트 코드로 공개 프로필(닉네임, 한 줄 소개, 링크, 프로필 사진)을 조회합니다. 로그인이 필요하지 않습니다. "
            + "존재하지 않는 코드와 탈퇴한 호스트를 구분하지 않고 404로 응답합니다.")
    public ResponseEntity<ApiResponse<PublicHostProfileResponse>> getPublicProfile(
        @PathVariable(name = "hostCode") String hostCode
    ) {
        var response = hostService.getPublicProfile(hostCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/auth/onboarding")
    @Operation(summary = "온보딩 완료",
        description = "서비스 닉네임과 약관 동의 이력을 함께 저장합니다. " +
            "이미 온보딩이 완료된 호스트가 다시 호출하면 409 Conflict를 반환합니다.")
    public ResponseEntity<ApiResponse<HostResponse>> submitOnboarding(
        @LoginHost Host host,
        @RequestBody OnboardingRequest request
    ) {
        var response = hostService.submitOnboarding(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/auth/me")
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
