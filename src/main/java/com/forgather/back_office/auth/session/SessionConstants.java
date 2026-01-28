package com.forgather.back_office.auth.session;

public final class SessionConstants {

    public static final String SESSION_COOKIE_NAME = "ADMIN_SESSION_ID";
    public static final int SESSION_DURATION_SECONDS = 30 * 60;  // 30분

    private SessionConstants() {
    }
}
