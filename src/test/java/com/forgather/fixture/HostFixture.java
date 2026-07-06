package com.forgather.fixture;

import org.springframework.test.util.ReflectionTestUtils;

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

    public static Host createHostWithId(long id) {
        Host host = new Host("포스티", "pictureUrl");
        ReflectionTestUtils.setField(host, "id", id);
        return host;
    }
}
