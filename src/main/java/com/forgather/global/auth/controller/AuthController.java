package com.forgather.global.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.dto.HostResponse;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.service.HostAccountService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.RefreshRequest;
import com.forgather.global.auth.service.AuthService;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth: 인증", description = "인증 관련 API")
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final HostAccountService hostAccountService;
    private final AuthCookieProvider authCookieProvider;

    @GetMapping("/me")
    @Operation(summary = "내 정보 확인",
        description = "현재 로그인된 사용자의 정보를 확인합니다. " +
            "로그인된 사용자가 없으면 401 Unauthorized를 반환합니다.")
    public ResponseEntity<ApiResponse<HostResponse>> getCurrentUser(@LoginHost Host host) {
        var response = hostAccountService.getAccount(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login/kakao/confirm")
    @Operation(summary = "Kakao 로그인 완료",
        description = "Kakao 로그인 후 발급받은 액세스토큰을 전달하여 로그인합니다. " +
            "로그인 성공 시, 액세스토큰과 리프레시토큰을 응답 바디와 HttpOnly 쿠키로 반환합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLoginConfirm(
        @RequestBody KakaoLoginConfirmRequest request
    ) {
        var response = authService.kakaoLoginConfirm(request);
        return createTokenResponse(response);
    }

    @PostMapping("/login/apple/confirm")
    @Operation(summary = "Apple 로그인 완료",
        description = "Apple 로그인 후 발급받은 identity token, authorization code, raw nonce와 이름을 전달합니다. " +
            "서버는 authorization code를 Apple token endpoint에 교환하여 로그인합니다. " +
            "로그인 성공 시, 액세스토큰과 리프레시토큰을 응답 바디와 HttpOnly 쿠키로 반환합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> appleLoginConfirm(
        @RequestBody AppleLoginConfirmRequest request
    ) {
        var response = authService.appleLoginConfirm(request);
        return createTokenResponse(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "로그인 세션 갱신",
        description = "요청 바디의 리프레시 토큰을 우선 사용하고, 없으면 쿠키의 리프레시 토큰으로 " +
            "로그인 세션을 갱신합니다. 갱신된 토큰은 응답 바디와 HttpOnly 쿠키로 반환합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
        @RequestBody(required = false) RefreshRequest request,
        @Parameter(hidden = true)
        @CookieValue(name = AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME, required = false)
        String refreshTokenCookie
    ) {
        String refreshToken = resolveRefreshToken(request, refreshTokenCookie);
        var response = authService.refresh(refreshToken);
        return createTokenResponse(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃",
        description = "액세스토큰과 리프레시토큰 쿠키를 만료시킵니다. 인증 없이 호출할 수 있습니다. " +
            "stateless JWT 구조이므로 서버 측에서 발급된 토큰을 무효화하지는 않습니다. " +
            "클라이언트가 보관 중인 토큰은 직접 폐기해야 합니다.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return createExpiredCookieResponse();
    }

    private String resolveRefreshToken(RefreshRequest request, String refreshTokenCookie) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            return request.refreshToken();
        }
        if (StringUtils.hasText(refreshTokenCookie)) {
            return refreshTokenCookie;
        }
        throw new UnauthorizedException("리프레시 토큰이 필요합니다.");
    }

    private ResponseEntity<ApiResponse<Void>> createExpiredCookieResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieProvider.expireAccessTokenCookie().toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookieProvider.expireRefreshTokenCookie().toString());
        return ResponseEntity.ok()
            .headers(headers)
            .body(ApiResponse.success());
    }

    private ResponseEntity<ApiResponse<LoginResponse>> createTokenResponse(LoginResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(
            HttpHeaders.SET_COOKIE,
            authCookieProvider.createAccessTokenCookie(response.accessToken()).toString()
        );
        headers.add(
            HttpHeaders.SET_COOKIE,
            authCookieProvider.createRefreshTokenCookie(response.refreshToken()).toString()
        );
        return ResponseEntity.ok()
            .headers(headers)
            .body(ApiResponse.success(response));
    }
}
