package com.forgather.fixture;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.space.model.Space;
import com.forgather.global.auth.model.SpaceHost;

public class SpaceHostFixture {

    public static SpaceHost createSpaceHostWithSpaceAndHost(Space space, Host host) {
        return new SpaceHost(space, host);
    }

    public static SpaceHost createSpaceHost() {
        return createSpaceHostWithSpaceAndHost(SpaceFixture.createSpace(), HostFixture.createHost());
    }
}
