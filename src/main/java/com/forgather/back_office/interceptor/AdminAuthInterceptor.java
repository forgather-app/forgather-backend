package com.forgather.back_office.interceptor;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.UnauthorizedException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_USER_ID_ATTRIBUTE = "adminUserId";

    private static final String MDC_ADMIN_USER_ID_KEY = "adminUserId";

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        String sessionIdValue = extractSessionIdFromCookie(request);

        if (sessionIdValue == null) {
            log.info("어드민 페이지 접근 시도 - 세션 쿠키 없음: {}", request.getRequestURI());
            handleUnauthorized(request, response);
            return false;
        }

        try {
            SessionId sessionId = SessionId.from(sessionIdValue);
            AdminSession session = sessionManager.getValidSession(sessionId, LocalDateTime.now());
            request.setAttribute(ADMIN_USER_ID_ATTRIBUTE, session.getAdminUserId());
            MDC.put(MDC_ADMIN_USER_ID_KEY, String.valueOf(session.getAdminUserId()));
            log.info("어드민 페이지 접근 허용 - URI: {}", request.getRequestURI());
            return true;
        } catch (BaseException e) {
            // 예상된 인증 실패만 처리한다. 그 외 예외는 GlobalExceptionHandler가 처리하도록 전파한다.
            log.info("어드민 페이지 접근 실패 - {}: {}", e.getMessage(), request.getRequestURI());
            handleUnauthorized(request, response);
            return false;
        }
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception exception
    ) {
        MDC.remove(MDC_ADMIN_USER_ID_KEY);
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

    private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response) {
        if (isViewRequest(request)) {
            redirectToLogin(response);
            return;
        }
        throw new UnauthorizedException("인증이 필요합니다.");
    }

    private boolean isViewRequest(HttpServletRequest request) {
        return request.getRequestURI()
            .startsWith("/view");
    }

    private void redirectToLogin(HttpServletResponse response) {
        try {
            response.sendRedirect("/view/admin/login");
        } catch (IOException e) {
            log.error("로그인 페이지 리다이렉트 실패", e);
        }
    }
}
