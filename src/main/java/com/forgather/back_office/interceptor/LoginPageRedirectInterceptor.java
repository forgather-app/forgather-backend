package com.forgather.back_office.interceptor;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.model.SessionId;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoginPageRedirectInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        if (hasValidSession(request)) {
            log.info("이미 로그인된 사용자 - spaces 페이지로 리다이렉트");
            response.sendRedirect("/view/admin/spaces");
            return false;
        }
        return true;
    }

    private boolean hasValidSession(HttpServletRequest request) {
        String sessionIdValue = extractSessionIdFromCookie(request);
        if (sessionIdValue == null) {
            return false;
        }

        try {
            SessionId sessionId = SessionId.from(sessionIdValue);
            sessionManager.getValidSession(sessionId, LocalDateTime.now());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
