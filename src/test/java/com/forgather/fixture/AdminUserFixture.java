package com.forgather.fixture;

import com.forgather.back_office.model.AdminUser;

public class AdminUserFixture {

    // 테스트용 고정 해시 (평문: "password")
    private static final String DEFAULT_HASHED_PASSWORD =
        "$2a$10$mNeHrRkfqB23eiSOu7tMt.CrCoByLJwDYx9fU.gL1n4IL5Ff8m.PG";

    public static final String RAW_PASSWORD = "password";

    private AdminUserFixture() {
    }

    public static AdminUser createAdminUser() {
        return new AdminUser("admin", DEFAULT_HASHED_PASSWORD);
    }

    public static AdminUser createAdminUser(String username) {
        return new AdminUser(username, DEFAULT_HASHED_PASSWORD);
    }

    public static AdminUser createAdminUser(String username, String hashedPassword) {
        return new AdminUser(username, hashedPassword);
    }
}
