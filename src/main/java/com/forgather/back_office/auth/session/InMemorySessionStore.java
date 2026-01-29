package com.forgather.back_office.auth.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;
import com.forgather.global.exception.NotFoundException;

@Component
public class InMemorySessionStore implements SessionStore {

    private final Map<SessionId, AdminSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(AdminSession session) {
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public AdminSession getBySessionId(SessionId sessionId) {
        AdminSession session = sessions.get(sessionId);
        if (session == null) {
            throw new NotFoundException("어드민 세션이 존재하지 않습니다.");
        }
        return session;
    }

    @Override
    public void delete(SessionId sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public int deleteExpiredSessions(LocalDateTime standardDateTime) {
        List<SessionId> expiredSessionIds = sessions.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isExpired(standardDateTime))
            .map(Map.Entry::getKey)
            .toList();

        expiredSessionIds.forEach(sessions::remove);
        return expiredSessionIds.size();
    }
}
