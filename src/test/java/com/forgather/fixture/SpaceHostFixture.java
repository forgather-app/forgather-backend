package com.forgather.fixture;

import com.forgather.domain.space.model.Space;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;

public class SpaceHostFixture {

    public static SpaceHost createSpaceHostWithSpaceAndAppUser(Space space, AppUser appUser) {
        return new SpaceHost(space, appUser);
    }

    public static SpaceHost createSpaceHost() {
        return createSpaceHostWithSpaceAndAppUser(SpaceFixture.createSpace(), AppUserFixture.createAppUser());
    }
}
