package com.forgather.back_office.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.interceptor.AdminAuthInterceptor;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.global.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginAdminUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AdminUserRepository adminUserRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Admin.class);
    }

    @Override
    public AdminUser resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
        Long adminUserId = (Long)request.getAttribute(AdminAuthInterceptor.ADMIN_USER_ID_ATTRIBUTE);

        if (adminUserId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return adminUserRepository.getByIdOrThrow(adminUserId);
    }
}

