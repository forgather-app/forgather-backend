package com.forgather.domain.auth.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.auth.dto.LoginResponse;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile({"local", "dev", "test"})
@Tag(name = "Auth: 개발용 임시 로그인", description = "운영(prod) 환경에는 등록되지 않는 임시 로그인 API")
@RequestMapping("/auth")
public class DevAuthController {

    private final DevAuthService devAuthService;
    private final AuthCookieProvider authCookieProvider;

    @PostMapping("/login/dev")
    @Operation(summary = "개발용 임시 로그인",
        description = "설정된 고정 아이디/비밀번호로 로그인합니다. 카카오 로그인과 동일하게 " +
            "액세스토큰과 리프레시토큰을 응답 바디와 HttpOnly 쿠키로 반환합니다. " +
            "운영 환경에는 이 API가 존재하지 않습니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> devLogin(@RequestBody DevLoginRequest request) {
        LoginResponse response = devAuthService.login(request);
        return createTokenResponse(response);
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
