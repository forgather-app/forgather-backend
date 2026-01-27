package com.forgather.back_office.auth.session;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
            throw new UnauthorizedException("세션이 만료되었습니다.");
        }
        AdminSession refreshedSession = session.refresh();
        sessionStore.save(refreshedSession);
        return refreshedSession;
    }

    public void invalidateSession(SessionId sessionId) {
        sessionStore.delete(sessionId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredSessions() {
        List<AdminSession> sessions = sessionStore.getAllSessions();
        LocalDateTime now = LocalDateTime.now();

        int deletedCount = 0;
        for (AdminSession session : sessions) {
            if (session.isExpired(now)) {
                sessionStore.delete(session.getSessionId());
                deletedCount++;
            }
        }
        log.info("만료된 세션 정리 완료: {}개 삭제", deletedCount);
    }
}
