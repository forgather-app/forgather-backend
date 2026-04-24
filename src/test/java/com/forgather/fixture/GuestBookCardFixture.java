package com.forgather.fixture;

import static com.forgather.fixture.SpaceFixture.createSpace;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.space.model.Space;

public class GuestBookCardFixture {

    private static final Space space = createSpace();

    public static GuestBookCard createGuestBookCard() {
        return new GuestBookCard(space, "nickname", "message");
    }

    public static GuestBookCard createGuestBookCard(Space space, String nickname, String message) {
        return new GuestBookCard(space, nickname, message);
    }

    public static GuestBookCard createGuestBookCardWithSpace(Space space) {
        return createGuestBookCard(space, "nickname", "message");
    }

    public static GuestBookCard createGuestBookCardWithNickname(String nickname) {
        return createGuestBookCard(space, nickname, "message");
    }

    public static GuestBookCard createGuestBookCardWithMessage(String message) {
        return createGuestBookCard(space, "nickname", message);
    }

    public static GuestBookCard createGuestBookCardWithSpaceAndNickname(Space space, String nickname) {
        return createGuestBookCard(space, nickname, "message");
    }
}
