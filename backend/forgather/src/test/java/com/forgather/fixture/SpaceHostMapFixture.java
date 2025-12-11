package com.forgather.fixture;

import com.forgather.global.auth.model.SpaceHostMap;

public class SpaceHostMapFixture {

    public static SpaceHostMap createSpaceHostMap() {
        return new SpaceHostMap(SpaceFixture.createSpace(), HostFixture.createHost());
    }
}
