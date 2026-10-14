package com.forgather.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.forgather.global.util.RandomCodeGenerator;

class LoggingInterceptorTest {

    private final LoggingInterceptor loggingInterceptor = new LoggingInterceptor(new RandomCodeGenerator());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @DisplayName("어드민 페이지와 API를 요청하면 요청 완료 시까지 어드민 로그 유형이 설정된다.")
    @ParameterizedTest
    @ValueSource(strings = {
        "/admin/login", "/admin/logout", "/admin/spaces", "/view/admin/login", "/view/admin/spaces"
    })
    void adminRequestHasLogTypeUntilCompletion(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();

        loggingInterceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("logType")).isEqualTo("admin");

        loggingInterceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("logType")).isNull();
    }

    @DisplayName("일반 사용자 경로를 요청하면 이전 요청의 어드민 로그 유형이 제거된다.")
    @Test
    void publicRequestDoesNotInheritAdminLogType() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("logType", "admin");

        loggingInterceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("logType")).isNull();

        loggingInterceptor.afterCompletion(request, response, new Object(), null);
    }
}
