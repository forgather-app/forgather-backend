package com.forgather.domain.guestbook.service;

import static com.forgather.fixture.GuestBookCardPhotoFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpaceHost;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.GuestBookCardPhotoFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Import({GuestBookService.class, FakeContentStorage.class})
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@DataJpaTest
public class GuestBookDeleteServiceTest {

    @Autowired
    private GuestBookService guestBookService;

    @Autowired
    GuestBookCardRepository guestBookCardRepository;

    @Autowired
    GuestBookCardPhotoRepository guestBookCardPhotoRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    private Host host;
    private Space space;

    @BeforeEach
    void setUp() {
        space = SpaceFixture.createSpace();
        spaceRepository.save(space);

        host = HostFixture.createHost();
        hostRepository.save(host);

        spaceHostRepository.save(new SpaceHost(space, host));
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
        guestBookService.deleteCard(host, space.getCode(), guestBookCard1.getId());
        guestBookService.deleteCard(host, space.getCode(), guestBookCard3.getId());

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

    @DisplayName("주어진 스페이스에 속하는 모든 방명록과 사진을 논리 삭제한다")
    @Test
    void softDeleteGuestBookBySpace() {
        // given
        GuestBookCard guestBookCard1 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test1"));
        GuestBookCard guestBookCard2 = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test2"));
        guestBookCardPhotoRepository.saveAll(List.of(
            createGuestBookCardPhotoWithGuestBookCard(guestBookCard1),
            createGuestBookCardPhotoWithGuestBookCard(guestBookCard2)
        ));

        // when
        guestBookService.deleteAllCardsBySpace(host, space);

        // then
        assertAll(
            () -> assertThat(guestBookCardRepository.count()).isEqualTo(2),
            () -> assertThat(guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space)).isEmpty(),
            () -> assertThat(guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard1))
                .isEmpty(),
            () -> assertThat(guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard2))
                .isEmpty()
        );
    }

    @DisplayName("스페이스 방명록 일괄 삭제 시 삭제 시각이 updatedAt에도 반영된다")
    @Test
    void softDeleteGuestBookBySpaceUpdatesUpdatedAt() {
        // given
        GuestBookCard guestBookCard = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "test"));
        GuestBookCardPhoto photo = guestBookCardPhotoRepository.saveAll(
            List.of(createGuestBookCardPhotoWithGuestBookCard(guestBookCard))).getFirst();

        // when
        guestBookService.deleteAllCardsBySpace(host, space);

        // then
        // 벌크 UPDATE는 1차 캐시에 반영되지 않으므로 DB 값으로 다시 읽는다
        entityManager.refresh(guestBookCard);
        entityManager.refresh(photo);
        assertAll(
            () -> assertThat(guestBookCard.getDeletedAt()).isNotNull(),
            () -> assertThat(guestBookCard.getUpdatedAt()).isEqualTo(guestBookCard.getDeletedAt()),
            () -> assertThat(photo.getDeletedAt()).isNotNull(),
            () -> assertThat(photo.getUpdatedAt()).isEqualTo(photo.getDeletedAt())
        );
    }

    @DisplayName("스페이스 방명록 일괄 삭제는 다른 스페이스의 방명록에 영향을 주지 않는다")
    @Test
    void softDeleteGuestBookBySpaceDoesNotAffectOtherSpace() {
        // given
        Space otherSpace = spaceRepository.save(SpaceFixture.createSpaceWithCode("other12345"));
        guestBookCardRepository.save(new GuestBookCard(space, "nickname", "mine"));
        GuestBookCard otherCard = guestBookCardRepository.save(new GuestBookCard(otherSpace, "nickname", "other"));
        guestBookCardPhotoRepository.saveAll(List.of(createGuestBookCardPhotoWithGuestBookCard(otherCard)));

        // when
        guestBookService.deleteAllCardsBySpace(host, space);

        // then
        assertAll(
            () -> assertThat(guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(otherSpace))
                .extracting(GuestBookCard::getId)
                .containsExactly(otherCard.getId()),
            () -> assertThat(guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(otherCard))
                .hasSize(1)
        );
    }

    @DisplayName("스페이스 방명록 일괄 삭제는 방명록 수와 무관하게 statement 3개(권한 검증 1 + 벌크 UPDATE 2)로 처리한다")
    @Test
    void softDeleteGuestBookBySpaceWithConstantStatements() {
        // given
        List<GuestBookCard> cards = IntStream.range(0, 30)
            .mapToObj(i -> new GuestBookCard(space, "nickname", "test" + i))
            .toList();
        cards.forEach(guestBookCardRepository::save);
        guestBookCardPhotoRepository.saveAll(cards.stream()
            .map(GuestBookCardPhotoFixture::createGuestBookCardPhotoWithGuestBookCard)
            .toList());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // when
        guestBookService.deleteAllCardsBySpace(host, space);

        // then
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }
}
