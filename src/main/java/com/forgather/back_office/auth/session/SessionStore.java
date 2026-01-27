package com.forgather.back_office.auth.session;

import com.forgather.back_office.model.AdminSession;

public interface SessionStore {

    void save(AdminSession session);
}
