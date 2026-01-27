package com.forgather.back_office.auth.session;

import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;

public interface SessionStore {

    void save(AdminSession session);

    AdminSession getBySessionId(SessionId sessionId);

    void delete(SessionId sessionId);
}
