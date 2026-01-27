package com.forgather.back_office.auth.session;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SessionManager {

    private final SessionStore sessionStore;
    private final RandomCodeGenerator randomCodeGenerator;

    public AdminSession createSession(Long adminUserId, String username) {
        AdminSession adminSession = AdminSession.create(adminUserId, username, randomCodeGenerator);
        sessionStore.save(adminSession);
        return adminSession;
    }

    public AdminSession getValidSession(SessionId sessionId, LocalDateTime standardDateTime) {
        AdminSession session = sessionStore.getBySessionId(sessionId);
        if (session.isExpired(standardDateTime)) {
            sessionStore.delete(sessionId);
            throw new IllegalStateException("세션이 만료되었습니다.");
        }
        AdminSession refreshedSession = session.refresh();
        sessionStore.save(refreshedSession);
        return refreshedSession;
    }

    public void invalidateSession(SessionId sessionId) {
        sessionStore.delete(sessionId);
    }
}
