package com.forgather.fixture;

import com.forgather.global.auth.model.AppUser;

public class AppUserFixture {
    public static AppUser createAppUser() {
        return new AppUser("포스티", "pictureUrl");
    }

    public static AppUser createAppUserWithName(String name) {
        return new AppUser(name, "pictureUrl");
    }
}
