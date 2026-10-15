package com.forgather.global.client;

import static com.forgather.global.client.ClientVersionInterceptor.APP_VERSION_HEADER;
import static com.forgather.global.client.ClientVersionInterceptor.CLIENT_TYPE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.forgather.global.exception.UpgradeRequiredException;

class ClientVersionInterceptorTest {

    private static final String IOS_MINIMUM_VERSION = "1.2.0";
    private static final String ANDROID_MINIMUM_VERSION = "1.3.0";

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final HandlerMethod handlerMethod = createHandlerMethod();

    @DisplayName("웹 요청은 버전을 검증하지 않는다.")
    @Test
    void webRequestBypassesVersionValidation() {
        MockHttpServletRequest request = createRequest("WEB", null);

        assertThat(enabledInterceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @DisplayName("플랫폼별 최소 지원 버전 이상인 앱 요청은 통과한다.")
    @Test
    void appRequestWithSupportedVersion() {
        ClientVersionInterceptor interceptor = enabledInterceptor();

        assertThat(interceptor.preHandle(createRequest("IOS", "1.2.0"), response, handlerMethod)).isTrue();
        assertThat(interceptor.preHandle(createRequest("IOS", "1.10.0"), response, handlerMethod)).isTrue();
        assertThat(interceptor.preHandle(createRequest("ANDROID", "1.3.0"), response, handlerMethod)).isTrue();
    }

    @DisplayName("같은 버전이라도 플랫폼별 최소 지원 버전에 따라 결과가 달라진다.")
    @Test
    void minimumVersionIsAppliedPerPlatform() {
        ClientVersionInterceptor interceptor = enabledInterceptor();

        assertThat(interceptor.preHandle(createRequest("IOS", "1.2.0"), response, handlerMethod)).isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(createRequest("ANDROID", "1.2.0"), response, handlerMethod))
            .isInstanceOf(UpgradeRequiredException.class)
            .hasMessageContaining(ANDROID_MINIMUM_VERSION);
    }

    @DisplayName("클라이언트 타입 헤더가 없는 요청은 구버전 앱으로 보고 차단한다.")
    @Test
    void requestWithoutClientTypeIsBlocked() {
        MockHttpServletRequest request = createRequest(null, null);

        assertThatThrownBy(() -> enabledInterceptor().preHandle(request, response, handlerMethod))
            .isInstanceOf(UpgradeRequiredException.class);
    }

    @DisplayName("알 수 없는 클라이언트 타입은 차단한다.")
    @Test
    void unknownClientTypeIsBlocked() {
        MockHttpServletRequest request = createRequest("APP", "9.9.9");

        assertThatThrownBy(() -> enabledInterceptor().preHandle(request, response, handlerMethod))
            .isInstanceOf(UpgradeRequiredException.class);
    }

    @DisplayName("앱 버전이 없거나 형식이 올바르지 않거나 최소 지원 버전 미만이면 차단한다.")
    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.2", "1.1.9"})
    void unsupportedAppVersionIsBlocked(String appVersion) {
        MockHttpServletRequest request = createRequest("IOS", appVersion);

        assertThatThrownBy(() -> enabledInterceptor().preHandle(request, response, handlerMethod))
            .isInstanceOf(UpgradeRequiredException.class);
    }

    @DisplayName("앱 버전 헤더가 없으면 차단한다.")
    @Test
    void requestWithoutAppVersionIsBlocked() {
        MockHttpServletRequest request = createRequest("ANDROID", null);

        assertThatThrownBy(() -> enabledInterceptor().preHandle(request, response, handlerMethod))
            .isInstanceOf(UpgradeRequiredException.class);
    }

    @DisplayName("CORS preflight 요청은 검증하지 않는다.")
    @Test
    void preflightRequestBypassesValidation() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/terms");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        assertThat(enabledInterceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @DisplayName("컨트롤러 메서드가 아닌 핸들러는 검증하지 않는다.")
    @Test
    void nonHandlerMethodBypassesValidation() {
        MockHttpServletRequest request = createRequest(null, null);

        assertThat(enabledInterceptor().preHandle(request, response, new Object())).isTrue();
    }

    @DisplayName("검증이 비활성화되어 있으면 모든 요청을 통과시킨다.")
    @Test
    void disabledInterceptorBypassesValidation() {
        ClientVersionInterceptor interceptor = new ClientVersionInterceptor(createProperties(false, "1.2.0", "1.3.0"));

        assertThat(interceptor.preHandle(createRequest(null, null), response, handlerMethod)).isTrue();
    }

    @DisplayName("최소 지원 버전 설정 형식이 올바르지 않으면 생성에 실패한다.")
    @Test
    void invalidMinimumVersionFailsCreation() {
        ClientVersionProperties properties = createProperties(true, "1.2.0", "invalid");

        assertThatThrownBy(() -> new ClientVersionInterceptor(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("android");
    }

    private ClientVersionInterceptor enabledInterceptor() {
        return new ClientVersionInterceptor(createProperties(true, IOS_MINIMUM_VERSION, ANDROID_MINIMUM_VERSION));
    }

    private ClientVersionProperties createProperties(boolean enabled, String ios, String android) {
        return new ClientVersionProperties(enabled, new ClientVersionProperties.MinimumAppVersion(ios, android));
    }

    private MockHttpServletRequest createRequest(String clientType, String appVersion) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/terms");
        if (clientType != null) {
            request.addHeader(CLIENT_TYPE_HEADER, clientType);
        }
        if (appVersion != null) {
            request.addHeader(APP_VERSION_HEADER, appVersion);
        }
        return request;
    }

    private HandlerMethod createHandlerMethod() {
        try {
            return new HandlerMethod(new Object(), Object.class.getMethod("toString"));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
