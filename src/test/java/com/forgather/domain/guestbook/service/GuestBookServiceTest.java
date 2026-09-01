package com.forgather.domain.guestbook.service;

import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_ADMIN;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_HOST;
import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCardWithSpace;
import static com.forgather.fixture.GuestBookCardFixture.createWithSpaceAndVisibilityStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.forgather.domain.guestbook.dto.GuestBookCardResponse;
import com.forgather.domain.guestbook.dto.GuestBookResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.NotFoundException;

@Import({GuestBookService.class, FakeContentStorage.class})
@DataJpaTest
class GuestBookServiceTest {

    @Autowired
    private GuestBookService guestBookService;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    private Space space;
    private Host host;

    @BeforeEach
    void setUp() {
        space = spaceRepository.save(SpaceFixture.createSpace());
        host = hostRepository.save(HostFixture.createHost());
        spaceHostRepository.save(new SpaceHost(space, host));
    }

    @DisplayName("방명록 목록 조회 시 숨김 처리되지 않은 방명록만 반환한다")
    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void readGuestBookExcludesHiddenCards() {
        // given
        GuestBookCard hiddenCard = guestBookCardRepository.save(createGuestBookCardWithSpace(space));
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

    @DisplayName("스페이스 호스트가 숨김 처리한 방명록 카드는 스페이스 호스트가 상세 조회할 수 있다")
    @Test
    void readHiddenByHostCardBySpaceHost() {
        // given
        GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(space, HIDDEN_BY_HOST);
        guestBookCardRepository.save(guestBookCard);

        // when
        GuestBookCardResponse result = guestBookService.readCard(host, space.getCode(), guestBookCard.getId());

        // then
        assertAll(
            () -> assertThat(result.id()).isEqualTo(guestBookCard.getId()),
            () -> assertThat(result.nickname()).isEqualTo(guestBookCard.getNickname())
        );
    }

    @DisplayName("호스트가 숨김 처리한 방명록 카드는 방문자가 상세 조회할 수 없다")
    @Test
    void throwExceptionWhenGuestReadHiddenByHostCard() {
        // given
        GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(space, HIDDEN_BY_HOST);
        guestBookCardRepository.save(guestBookCard);

        // when, then
        assertThatThrownBy(() -> guestBookService.readCard(null, space.getCode(), guestBookCard.getId()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 방명록 카드입니다.");
    }

    @DisplayName("관리자가 숨김 처리한 방명록 카드는 스페이스 호스트도 조회할 수 없다")
    @Test
    void throwExceptionWhenSpaceHostReadHiddenByAdminCard() {
        // given
        GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(space, HIDDEN_BY_ADMIN);
        guestBookCardRepository.save(guestBookCard);

        // when, then
        assertThatThrownBy(() -> guestBookService.readCard(host, space.getCode(), guestBookCard.getId()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 방명록 카드입니다.");
    }
}
