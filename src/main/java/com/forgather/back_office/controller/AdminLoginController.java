package com.forgather.back_office.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.model.SessionId;
import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.AdminLoginRequest;
import com.forgather.back_office.dto.AdminLoginResponse;
import com.forgather.back_office.service.AdminLoginService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION_ID";

    private final AdminLoginService adminLoginService;
    private final SessionManager sessionManager;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        var response = adminLoginService.login(request);

        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, response.sessionId())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(30 * 60)
            .sameSite("Strict")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId != null) {
            sessionManager.invalidateSession(SessionId.from(sessionId));
        }

        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }
}
