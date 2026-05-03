package com.forgather.fixture;

import com.forgather.global.auth.model.Host;

public class HostFixture {
    public static Host createHost() {
        return new Host("포스티", "pictureUrl");
    }

    public static Host createHostWithName(String name) {
        return new Host(name, "pictureUrl");
    }
}
