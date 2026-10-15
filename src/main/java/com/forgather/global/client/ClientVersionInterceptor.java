package com.forgather.global.client;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.forgather.global.exception.UpgradeRequiredException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ClientVersionInterceptor implements HandlerInterceptor {

    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    public static final String APP_VERSION_HEADER = "X-App-Version";

    private final boolean isEnabled;
    private final Map<ClientType, AppVersion> minimumVersions = new EnumMap<>(ClientType.class);

    public ClientVersionInterceptor(ClientVersionProperties properties) {
        this.isEnabled = properties.isEnabled();
        ClientVersionProperties.MinimumAppVersion minimumAppVersion = properties.getMinimumAppVersion();
        minimumVersions.put(ClientType.IOS, parseMinimumVersion("ios", minimumAppVersion.getIos()));
        minimumVersions.put(ClientType.ANDROID, parseMinimumVersion("android", minimumAppVersion.getAndroid()));
    }

    // 잘못된 설정값으로 모든 앱 요청이 차단되지 않도록 애플리케이션 기동 시점에 실패시킨다
    private AppVersion parseMinimumVersion(String platform, String value) {
        return AppVersion.parse(value)
            .orElseThrow(() -> new IllegalStateException(
                "client-version.minimum-app-version.%s 형식이 올바르지 않습니다. value: %s".formatted(platform, value)
            ));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 브라우저는 CORS preflight 요청에 커스텀 헤더를 싣지 않는다
        if (!isEnabled || CorsUtils.isPreFlightRequest(request) || !(handler instanceof HandlerMethod)) {
            return true;
        }

        String clientTypeHeader = request.getHeader(CLIENT_TYPE_HEADER);
        // 클라이언트 타입 헤더가 없는 요청은 헤더 도입 이전에 배포된 구버전 앱으로 간주한다
        ClientType clientType = ClientType.from(clientTypeHeader)
            .orElseThrow(() -> new UpgradeRequiredException(
                "앱 업데이트가 필요합니다. 클라이언트 타입: " + clientTypeHeader
            ));
        if (clientType == ClientType.WEB) {
            return true;
        }

        validateAppVersion(minimumVersions.get(clientType), request.getHeader(APP_VERSION_HEADER));
        return true;
    }

    private void validateAppVersion(AppVersion minimumVersion, String appVersionHeader) {
        boolean isSupported = AppVersion.parse(appVersionHeader)
            .map(appVersion -> !appVersion.isLowerThan(minimumVersion))
            .orElse(false);
        if (!isSupported) {
            throw new UpgradeRequiredException(
                "앱 업데이트가 필요합니다. 최소 지원 버전: %s, 요청 버전: %s".formatted(minimumVersion, appVersionHeader)
            );
        }
    }
}
