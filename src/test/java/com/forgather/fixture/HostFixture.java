package com.forgather.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.global.auth.model.Host;

public class HostFixture {

    private static final String EMAIL = "postie@forgather.app";

    public static Host createHost() {
        Host host = new Host("포스티", EMAIL);
        host.updateNickname("포스티");
        return host;
    }

    public static Host createHostWithName(String name) {
        Host host = new Host(name, EMAIL);
        host.updateNickname(name);
        return host;
    }

    public static Host createHostWithId(long id) {
        Host host = new Host("포스티", EMAIL);
        ReflectionTestUtils.setField(host, "id", id);
        return host;
    }
}
