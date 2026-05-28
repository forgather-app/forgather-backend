package com.forgather.fixture;

import static com.forgather.fixture.SpaceFixture.createSpace;

import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.VisibilityStatus;
import com.forgather.domain.space.model.Space;

public class GuestBookCardFixture {

    public static final String NICKNAME = "nickname";
    public static final String MESSAGE = "message";
    private static final Space space = createSpace();

    public static GuestBookCard createGuestBookCard() {
        return new GuestBookCard(space, NICKNAME, MESSAGE);
    }

    public static GuestBookCard createWithVisibilityStatus(VisibilityStatus visibilityStatus) {
        GuestBookCard guestBookCard = new GuestBookCard(space, NICKNAME, MESSAGE);
        setVisibiliyStatus(visibilityStatus, guestBookCard);
        return guestBookCard;
    }

    public static GuestBookCard createGuestBookCard(Space space, String nickname, String message) {
        return new GuestBookCard(space, nickname, message);
    }

    public static GuestBookCard createGuestBookCardWithSpace(Space space) {
        return createGuestBookCard(space, NICKNAME, MESSAGE);
    }

    public static GuestBookCard createGuestBookCardWithNickname(String nickname) {
        return createGuestBookCard(space, nickname, MESSAGE);
    }

    public static GuestBookCard createGuestBookCardWithMessage(String message) {
        return createGuestBookCard(space, NICKNAME, message);
    }

    public static GuestBookCard createGuestBookCardWithSpaceAndNickname(Space space, String nickname) {
        return createGuestBookCard(space, nickname, MESSAGE);
    }

    public static GuestBookCard createWithSpaceAndVisibilityStatus(
        Space space,
        VisibilityStatus visibilityStatus
    ) {
        GuestBookCard guestBookCard = createGuestBookCard(space, NICKNAME, MESSAGE);
        setVisibiliyStatus(visibilityStatus, guestBookCard);
        return guestBookCard;
    }

    private static void setVisibiliyStatus(VisibilityStatus visibilityStatus, GuestBookCard guestBookCard) {
        ReflectionTestUtils.setField(guestBookCard, "visibilityStatus", visibilityStatus);
    }
}
