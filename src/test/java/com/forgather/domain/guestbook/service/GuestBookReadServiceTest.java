package com.forgather.domain.guestbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.forgather.domain.guestbook.dto.GuestBookResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.SpaceFixture;

@Import({GuestBookService.class, FakeContentStorage.class})
@DataJpaTest
class GuestBookReadServiceTest {

    @Autowired
    private GuestBookService guestBookService;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    private Space space;

    @BeforeEach
    void setUp() {
        space = spaceRepository.save(SpaceFixture.createSpace());
    }

    @DisplayName("방명록 목록 조회 시 숨김 처리되지 않은 방명록만 반환한다")
    @Test
    void readGuestBookExcludesHiddenCards() {
        // given
        GuestBookCard hiddenCard = guestBookCardRepository.save(new GuestBookCard(space, "hidden", "hidden message"));
        GuestBookCard visibleCard = guestBookCardRepository.save(new GuestBookCard(space, "visible", "visible message"));
        hiddenCard.hideByAdmin();

        // when
        GuestBookResponse result = guestBookService.read(
            null,
            space.getCode(),
            PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        // then
        assertAll(
            () -> assertThat(result.guestBookCards()).hasSize(1),
            () -> assertThat(result.guestBookCards().getFirst().id()).isEqualTo(visibleCard.getId()),
            () -> assertThat(result.totalCount()).isEqualTo(1),
            () -> assertThat(result.totalPages()).isEqualTo(1)
        );
    }
}
