package com.forgather.domain.guestbook.service;

import static com.forgather.fixture.GuestBookCardPhotoFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.AppUserFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;

@Import({GuestBookService.class, FakeContentStorage.class})
@DataJpaTest
public class GuestBookDeleteServiceTest {

    @Autowired
    private GuestBookService guestBookService;

    @Autowired
    GuestBookCardRepository guestBookCardRepository;

    @Autowired
    GuestBookCardPhotoRepository guestBookCardPhotoRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    private AppUser user;
    private Space space;

    @BeforeEach
    void setUp() {
        space = SpaceFixture.createSpace();
        spaceRepository.save(space);

        user = AppUserFixture.createAppUser();
        userRepository.save(user);

        spaceHostRepository.save(new SpaceHost(space, user));
    }

    @DisplayName("지정한 방명록을 논리 삭제한다")
    @Test
    void softDeleteGuestBookCard() {
        // given
        GuestBookCard guestBookCard1 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test1"));
        GuestBookCard guestBookCard2 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test2"));
        GuestBookCard guestBookCard3 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test3"));
        GuestBookCardPhoto guestBookCardPhoto1 = createGuestBookCardPhotoWithGuestBookCard(guestBookCard1);
        GuestBookCardPhoto guestBookCardPhoto2 = createGuestBookCardPhotoWithGuestBookCard(guestBookCard1);
        guestBookCardPhotoRepository.saveAll(List.of(guestBookCardPhoto1, guestBookCardPhoto2));

        // when
        guestBookService.deleteCard(user, space.getCode(), guestBookCard1.getId());
        guestBookService.deleteCard(user, space.getCode(), guestBookCard3.getId());

        // then
        assertAll(
            () -> assertThat(guestBookCardRepository.count()).isEqualTo(3),
            () -> assertThat(guestBookCard1.getDeletedAt()).isNotNull(),
            () -> assertThat(guestBookCard2.getDeletedAt()).isNull(),
            () -> assertThat(guestBookCard3.getDeletedAt()).isNotNull(),

            () -> assertThat(guestBookCardPhoto1.getDeletedAt()).isNotNull(),
            () -> assertThat(guestBookCardPhoto2.getDeletedAt()).isNotNull(),

            () -> assertThat(guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space))
                .extracting(GuestBookCard::getId)
                .containsExactly(guestBookCard2.getId())
        );
    }

    @DisplayName("주어진 스페이스에 속하는 모든 방명록을 논리 삭제한다")
    @Test
    void softDeleteGuestBookBySpace() {
        // given
        GuestBookCard guestBookCard1 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test1"));
        GuestBookCard guestBookCard2 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test2"));
        GuestBookCard guestBookCard3 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test3"));

        // when
        guestBookService.deleteAllCardsBySpace(user, space);

        // then
        assertAll(
            () -> assertThat(guestBookCardRepository.count()).isEqualTo(3),
            () -> assertThat(guestBookCard1.getDeletedAt()).isNotNull(),
            () -> assertThat(guestBookCard2.getDeletedAt()).isNotNull(),
            () -> assertThat(guestBookCard3.getDeletedAt()).isNotNull(),

            () -> assertThat(guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space)).isEmpty()
        );
    }
}
