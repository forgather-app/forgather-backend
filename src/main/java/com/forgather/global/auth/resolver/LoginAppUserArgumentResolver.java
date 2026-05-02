package com.forgather.global.auth.resolver;

import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.global.auth.annotation.LoginAppUser;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.UnauthorizedException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginAppUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String HOST = "HOST";

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository appUserRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginAppUser.class);
    }

    @Override
    public AppUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        LoginAppUser annotation = parameter.getParameterAnnotation(LoginAppUser.class);
        boolean required = Objects.requireNonNull(annotation).required();
        HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();

        String jwtToken = request.getHeader(AUTHORIZATION_HEADER_NAME);
        if (jwtToken == null) {
            throwExceptionIfRequired(required);
            return null;
        }
        if (!jwtToken.startsWith(BEARER)) {
            throw new JwtException("Invalid JWT token format");
        }

        jwtToken = jwtToken.substring(BEARER.length());
        jwtTokenProvider.validateToken(jwtToken);
        if (!jwtTokenProvider.getRole(jwtToken).equals(HOST)) {
            throw new UnauthorizedException("사용자 로그인이 필요합니다.");
        }

        Long appUserId = jwtTokenProvider.getId(jwtToken);
        return appUserRepository.getByIdOrThrow(appUserId);
    }

    private void throwExceptionIfRequired(boolean required) {
        if (required) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
