package com.forgather.domain.guestbook.service;

import static com.forgather.fixture.GuestBookReportReasonFixture.createHiddenReason;
import static com.forgather.fixture.GuestBookReportReasonFixture.createReason;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static com.forgather.fixture.SpaceFixture.createSpaceWithCode;
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

import com.forgather.domain.guestbook.dto.CreateGuestBookReportRequest;
import com.forgather.domain.guestbook.dto.CreateGuestBookReportResponse;
import com.forgather.domain.guestbook.dto.ReportHistoryResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.model.VisibilityStatus;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.GuestBookReportRepository;
import com.forgather.domain.guestbook.repository.jpa.GuestBookReportReasonJpaRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.ForbiddenException;
import com.forgather.global.exception.NotFoundException;

@Import(GuestbookReportService.class)
@DataJpaTest
class GuestbookReportServiceTest {

    @Autowired
    private GuestbookReportService guestBookReportService;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private GuestBookReportRepository guestBookReportRepository;

    @Autowired
    private GuestBookReportReasonJpaRepository reportReasonRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    private Host host;
    private Space space;
    private GuestBookCard card;
    private GuestBookReportReason reason;

    @BeforeEach
    void setUp() {
        space = spaceRepository.save(createSpace());
        host = hostRepository.save(createHost());
        spaceHostRepository.save(new SpaceHost(space, host));
        card = guestBookCardRepository.save(new GuestBookCard(space, "nickname", "message"));
        reason = reportReasonRepository.save(createReason());
    }

    @DisplayName("정상 신고하면 신고 레코드가 저장되고 방명록이 숨김 처리된다")
    @Test
    void report() {
        // given
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

        // when
        CreateGuestBookReportResponse response = guestBookReportService.report(host, space.getCode(), card.getId(), request);

        // then
        assertAll(
            () -> assertThat(response.id()).isNotNull(),
            () -> assertThat(response.guestBookCardId()).isEqualTo(card.getId()),
            () -> assertThat(guestBookReportRepository.existsByGuestBookCardAndReporterUser(card, host)).isTrue(),
            () -> assertThat(card.getVisibilityStatus()).isEqualTo(VisibilityStatus.HIDDEN_BY_ADMIN)
        );
    }

    @DisplayName("스페이스 소유자가 아니면 신고할 수 없다")
    @Test
    void throwExceptionWhenNotSpaceHost() {
        // given
        Host anotherHost = hostRepository.save(createHost());
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

        // when & then
        assertThatThrownBy(() -> guestBookReportService.report(anotherHost, space.getCode(), card.getId(), request))
            .isInstanceOf(ForbiddenException.class);
    }

    @DisplayName("해당 스페이스의 방명록이 아니면 신고할 수 없다")
    @Test
    void throwExceptionWhenCardNotBelongToSpace() {
        // given
        Space anotherSpace = spaceRepository.save(createSpaceWithCode("ANOTHER123"));
        spaceHostRepository.save(new SpaceHost(anotherSpace, host));
        GuestBookCard anotherCard = guestBookCardRepository.save(new GuestBookCard(anotherSpace, "nick", "msg"));
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

        // when & then
        assertThatThrownBy(() -> guestBookReportService.report(host, space.getCode(), anotherCard.getId(), request))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("이미 신고한 방명록은 재신고할 수 없다")
    @Test
    void throwExceptionWhenAlreadyReported() {
        // given
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);
        guestBookReportService.report(host, space.getCode(), card.getId(), request);

        // when & then
        assertThatThrownBy(() -> guestBookReportService.report(host, space.getCode(), card.getId(), request))
            .isInstanceOf(ConflictException.class);
    }

    @DisplayName("존재하지 않는 신고 사유로는 신고할 수 없다")
    @Test
    void throwExceptionWhenReasonNotFound() {
        // given
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(999L, null);

        // when & then
        assertThatThrownBy(() -> guestBookReportService.report(host, space.getCode(), card.getId(), request))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("숨김 처리된 신고 사유로는 신고할 수 없다")
    @Test
    void throwExceptionWhenReasonIsHidden() {
        // given
        GuestBookReportReason hiddenReason = createHiddenReason();
        reportReasonRepository.save(hiddenReason);
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(hiddenReason.getId(), null);

        // when & then
        assertThatThrownBy(() -> guestBookReportService.report(host, space.getCode(), card.getId(), request))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("신고 내역이 없으면 빈 페이지를 반환한다")
    @Test
    void retrieveReportHistoryEmpty() {
        ReportHistoryResponse result = guestBookReportService.retrieveReportHistory(
            host, PageRequest.of(0, 15)
        );

        assertThat(result.reportHistory()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    @DisplayName("자신의 신고 내역을 반환한다")
    @Test
    void retrieveReportHistory() {
        // given
        guestBookReportService.report(host, space.getCode(), card.getId(),
            new CreateGuestBookReportRequest(reason.getId(), null));

        // when
        ReportHistoryResponse result = guestBookReportService.retrieveReportHistory(
            host, PageRequest.of(0, 15)
        );

        // then
        assertAll(
            () -> assertThat(result.totalCount()).isEqualTo(1),
            () -> assertThat(result.reportHistory().get(0).nicknameSnapshot()).isEqualTo(card.getNickname()),
            () -> assertThat(result.reportHistory().get(0).messageSnapshot()).isEqualTo(card.getMessage())
        );
    }

    @DisplayName("Pageable 정렬 조건이 적용된다")
    @Test
    void retrieveReportHistoryWithSort() {
        // given
        GuestBookCard card2 = guestBookCardRepository.save(new GuestBookCard(space, "nick2", "msg2"));
        guestBookReportService.report(host, space.getCode(), card.getId(),
            new CreateGuestBookReportRequest(reason.getId(), null));
        guestBookReportService.report(host, space.getCode(), card2.getId(),
            new CreateGuestBookReportRequest(reason.getId(), null));

        // when
        ReportHistoryResponse result = guestBookReportService.retrieveReportHistory(
            host, PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "id"))
        );

        // then
        assertAll(
            () -> assertThat(result.totalCount()).isEqualTo(2),
            () -> assertThat(result.reportHistory().get(0).messageSnapshot()).isEqualTo(card2.getMessage()),
            () -> assertThat(result.reportHistory().get(1).messageSnapshot()).isEqualTo(card.getMessage())
        );
    }

    @DisplayName("다른 호스트의 신고 내역은 포함되지 않는다")
    @Test
    void retrieveReportHistoryExcludesOtherHost() {
        // given
        Host anotherHost = hostRepository.save(createHost());
        Space anotherSpace = spaceRepository.save(createSpaceWithCode("ANOTHER123"));
        spaceHostRepository.save(new SpaceHost(anotherSpace, anotherHost));
        GuestBookCard anotherCard = guestBookCardRepository.save(
            new GuestBookCard(anotherSpace, "nick", "msg")
        );
        guestBookReportService.report(anotherHost, anotherSpace.getCode(), anotherCard.getId(),
            new CreateGuestBookReportRequest(reason.getId(), null));

        // when
        ReportHistoryResponse result = guestBookReportService.retrieveReportHistory(
            host, PageRequest.of(0, 15)
        );

        // then
        assertThat(result.reportHistory()).isEmpty();
    }
}
