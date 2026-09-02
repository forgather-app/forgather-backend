package com.forgather.global.auth.resolver;

import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.WebUtils;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.UnauthorizedException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 모든 도메인 컨트롤러가 소비하는 인증 어댑터. Host 엔티티 반환으로 인한 domain 의존은
 * 인지된 예외이며 principal DTO 도입 시 제거 예정.
 */
@Component
@RequiredArgsConstructor
public class LoginHostArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String HOST = "HOST";

    private final JwtTokenProvider jwtTokenProvider;
    private final HostRepository hostRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginHost.class);
    }

    @Override
    public Host resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        LoginHost annotation = parameter.getParameterAnnotation(LoginHost.class);
        boolean required = Objects.requireNonNull(annotation).required();
        HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();

        String jwtToken = resolveJwtToken(request);
        if (jwtToken == null) {
            throwExceptionIfRequired(required);
            return null;
        }
        jwtTokenProvider.validateToken(jwtToken);
        if (!jwtTokenProvider.getRole(jwtToken).equals(HOST)) {
            throw new UnauthorizedException("호스트 로그인이 필요합니다.");
        }

        Long hostId = jwtTokenProvider.getId(jwtToken);
        return hostRepository.findByIdAndDeletedAtIsNull(hostId)
            .orElseThrow(() -> new UnauthorizedException("탈퇴했거나 존재하지 않는 호스트입니다. id: " + hostId));
    }

    private String resolveJwtToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER_NAME);
        if (authorizationHeader != null) {
            return extractBearerToken(authorizationHeader);
        }

        Cookie accessTokenCookie = WebUtils.getCookie(request, AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME);
        if (accessTokenCookie == null || !StringUtils.hasText(accessTokenCookie.getValue())) {
            return null;
        }
        return accessTokenCookie.getValue();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!authorizationHeader.startsWith(BEARER)) {
            throw new JwtException("Invalid JWT token format");
        }
        return authorizationHeader.substring(BEARER.length());
    }

    private void throwExceptionIfRequired(boolean required) {
        if (required) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
