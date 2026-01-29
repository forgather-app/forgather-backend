package com.forgather.back_office.controller;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static com.forgather.back_office.auth.session.SessionConstants.SESSION_DURATION_SECONDS;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.AdminLoginRequest;
import com.forgather.back_office.model.SessionId;
import com.forgather.back_office.service.AdminLoginService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminLoginService adminLoginService;
    private final SessionManager sessionManager;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody AdminLoginRequest request) {
        var response = adminLoginService.login(request);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createSessionCookie(response.sessionId(), SESSION_DURATION_SECONDS))
            .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId
    ) {
        if (sessionId != null) {
            sessionManager.invalidateSession(SessionId.from(sessionId));
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createSessionCookie("", 0))
            .build();
    }

    private String createSessionCookie(String value, int maxAge) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Strict")
            .build()
            .toString();
    }
}
