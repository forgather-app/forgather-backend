package com.forgather.back_office.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.AdminLoginRequest;
import com.forgather.back_office.dto.AdminLoginResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminUserRepository adminUserRepository;
    private final SessionManager sessionManager;

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser adminUser = adminUserRepository.findByUsername(request.username())
            .orElseThrow(() -> new BaseException("아이디나 패스워드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST));

        if (!adminUser.checkPassword(request.password())) {
            throw new BaseException("아이디나 패스워드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        AdminSession session = sessionManager.createSession(adminUser.getId(), adminUser.getUsername());
        return AdminLoginResponse.of(session.getSessionId().getValue());
    }
}
