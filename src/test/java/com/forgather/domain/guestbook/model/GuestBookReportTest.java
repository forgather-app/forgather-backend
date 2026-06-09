package com.forgather.domain.guestbook.model;

import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCard;
import static com.forgather.fixture.GuestBookReportFixture.createReport;
import static com.forgather.fixture.GuestBookReportFixture.createReportWithDetail;
import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.auth.model.Host;

class GuestBookReportTest {

    private final Host host = createHost();
    private final GuestBookCard card = createGuestBookCard();
    private final GuestBookReportReason reason = GuestBookReportReason.ADVERTISEMENT_SPAM;

    @DisplayName("방명록 카드가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenGuestBookCardIsNull() {
        // when & then
        assertThatThrownBy(() -> new GuestBookReport(null, host, host, ReporterType.HOST, reason, "기분 나빠요"))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("방명록 카드는 null일 수 없습니다.");
    }

    @DisplayName("호스트가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenHostUserIsNull() {
        // when & then
        assertThatThrownBy(() -> new GuestBookReport(card, null, host, ReporterType.HOST, reason, "기분 나빠요"))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("호스트는 null일 수 없습니다.");
    }

    @DisplayName("신고자가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenReporterUserIsNull() {
        // when & then
        assertThatThrownBy(() -> new GuestBookReport(card, host, null, ReporterType.HOST, reason, "기분 나빠요"))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("신고자는 null일 수 없습니다.");
    }

    @DisplayName("신고자 유형이 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenReporterTypeIsNull() {
        // when & then
        assertThatThrownBy(() -> new GuestBookReport(card, host, host, null, reason, "기분 나빠요"))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("신고자 유형은 null일 수 없습니다.");
    }

    @DisplayName("신고 사유가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenReasonIsNull() {
        // when & then
        assertThatThrownBy(() -> new GuestBookReport(card, host, host, ReporterType.HOST, null, "기분 나빠요"))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("신고 사유는 null일 수 없습니다.");
    }

    @DisplayName("상세 사유가 5자 미만이면 예외를 던진다")
    @Test
    void throwExceptionWhenDetailTooShort() {
        // given
        String detail = "1234";

        // when & then
        assertThatThrownBy(() -> createReportWithDetail(card, host, reason, detail))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("상세 사유는 최소 5자, 최대 200자까지 입력 가능합니다.");
    }

    @DisplayName("상세 사유가 200자를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenDetailExceedsMaxLength() {
        // given
        String detail = "a".repeat(201);

        // when & then
        assertThatThrownBy(() -> createReportWithDetail(card, host, reason, detail))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("상세 사유는 최소 5자, 최대 200자까지 입력 가능합니다.");
    }

    @DisplayName("상세 사유는 null이어도 생성할 수 있다")
    @Test
    void detailCanBeNull() {
        // when & then
        assertThatCode(() -> createReport(card, host, reason))
            .doesNotThrowAnyException();
    }

    @DisplayName("신고 시점의 닉네임과 메시지와 생성시간이 스냅샷으로 저장된다")
    @Test
    void snapshotsSavedFromCard() {
        // when
        GuestBookReport report = createReport(card, host, reason);

        // then
        assertAll(
            () -> assertThat(report.getNicknameSnapshot()).isEqualTo(card.getNickname()),
            () -> assertThat(report.getMessageSnapshot()).isEqualTo(card.getMessage()),
            () -> assertThat(report.getCreatedAtSnapshot()).isEqualTo(card.getCreatedAt())
        );
    }
}
