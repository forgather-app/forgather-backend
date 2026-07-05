package com.forgather.fixture;

import com.forgather.global.auth.model.Host;

public class HostFixture {
    public static Host createHost() {
        Host host = new Host("포스티", "pictureUrl");
        host.updateNickname("포스티");
        return host;
    }

    public static Host createHostWithName(String name) {
        Host host = new Host(name, "pictureUrl");
        host.updateNickname(name);
        return host;
    }
}
