package com.forgather.back_office.auth.session;

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
        if (!sessions.containsKey(sessionId)) {
            throw new NotFoundException("어드민 세션이 존재하지 않습니다.");
        }
        return sessions.get(sessionId);
    }

    @Override
    public void delete(SessionId sessionId) {
        sessions.remove(sessionId);
    }
}
