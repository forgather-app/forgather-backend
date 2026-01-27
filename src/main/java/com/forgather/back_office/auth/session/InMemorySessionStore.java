package com.forgather.back_office.auth.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.forgather.back_office.model.AdminSession;

@Component
public class InMemorySessionStore implements SessionStore {

    private final Map<String, AdminSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(AdminSession session) {
        sessions.put(session.getSessionIdValue(), session);
    }
}
