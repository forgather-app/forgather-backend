package com.forgather.global.logging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

import com.forgather.global.util.RandomCodeGenerator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "trace-id";
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_LOG_TYPE_KEY = "logType";
    private static final int TRACE_ID_LENGTH = 8;
    private static final String APPLICATION_JSON = "application/json";
    private static final Marker BODY_MARKER = MarkerFactory.getMarker("BODY");

    private final RandomCodeGenerator randomCodeGenerator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("com.forgather.startTime", System.currentTimeMillis());

        String traceId = extractTraceId(request);
        MDC.put(MDC_TRACE_ID_KEY, traceId); // 해당 쓰레드에서 발생하는 모든 로그에 포함
        if (isAdminRequest(request.getRequestURI())) {
            MDC.put(MDC_LOG_TYPE_KEY, "admin");
        } else {
            MDC.remove(MDC_LOG_TYPE_KEY);
        }

        log.atTrace()
            .addKeyValue("event", "REQUEST")
            .addKeyValue("httpMethod", request.getMethod())
            .addKeyValue("requestUri", request.getRequestURI())
            .addKeyValue("queryString", request.getQueryString())
            .addKeyValue("ip", getClientIp(request))
            .addKeyValue("userAgent", getUserAgent(request))
            .log();
        return true;
    }

    private boolean isAdminRequest(String requestUri) {
        return requestUri.equals("/admin") || requestUri.startsWith("/admin/")
            || requestUri.equals("/view/admin") || requestUri.startsWith("/view/admin/");
    }

    private String extractTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null) {
            return randomCodeGenerator.generate(TRACE_ID_LENGTH);
        }
        return traceId;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0]; // 여러 프록시 거친 경우 첫 IP가 실제 클라이언트
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "UnknownUserAgent";
        }
        return userAgent;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
        Exception exception) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(APPLICATION_JSON)
            && handler instanceof HandlerMethod handlerMethod) {
            LogRequestBody annotation = handlerMethod.getMethodAnnotation(LogRequestBody.class);
            if (annotation != null && log.isEnabledForLevel(annotation.level())) {
                logRequestBody(request, annotation.level());
            }
        }

        Long startTime = (Long)request.getAttribute("com.forgather.startTime");
        long durationMillis = (startTime != null) ? (System.currentTimeMillis() - startTime) : -1;

        log.atTrace()
            .addKeyValue("event", "RESPONSE")
            .addKeyValue("httpMethod", request.getMethod())
            .addKeyValue("requestUri", request.getRequestURI())
            .addKeyValue("queryString", request.getQueryString())
            .addKeyValue("duration", durationMillis + "ms")
            .log();

        setTraceIdHeader(response);
        MDC.clear(); // 쓰레드 종료 시 MDC 초기화
    }

    private void logRequestBody(HttpServletRequest request, Level level) {
        CustomRequestBodyWrapper wrapper = WebUtils.getNativeRequest(request, CustomRequestBodyWrapper.class);
        if (wrapper == null) {
            return;
        }
        try {
            byte[] bytes = wrapper.getInputStream().readAllBytes();
            if (bytes.length == 0) {
                return;
            }
            Charset charset = resolveLogCharset(request.getCharacterEncoding());
            log.atLevel(level)
                .addMarker(BODY_MARKER)
                .log("\n{}", new String(bytes, charset));
        } catch (IOException e) {
            log.warn("요청 본문 로깅에 실패했습니다.", e);
        }
    }

    private Charset resolveLogCharset(String encoding) {
        if (encoding == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private void setTraceIdHeader(HttpServletResponse response) {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        response.setHeader(TRACE_ID_HEADER, traceId);
    }
}
