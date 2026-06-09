package com.forgather.domain.guestbook.model;

import static com.forgather.domain.guestbook.model.VisibilityStatus.VISIBLE;
import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCard;
import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCardWithMessage;
import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCardWithSpace;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_ADMIN;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_HOST;
import static com.forgather.fixture.GuestBookCardFixture.createWithVisibilityStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.domain.guestbook.exception.GuestbookCardNotReadableException;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class GuestBookCardTest {

    @DisplayName("스페이스가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenSpaceIsNull() {
        // when, then
        assertThatThrownBy(() -> createGuestBookCardWithSpace(null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("방명록 카드 스페이스는 null일 수 없습니다.");
    }

    @DisplayName("메세지가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenMessageIsNull() {
        // when, then
        assertThatThrownBy(() -> createGuestBookCardWithMessage(null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("방명록 카드 메세지는 null일 수 없습니다.");
    }

    @DisplayName("메세지의 길이가 400자를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenMessageExceedMaxLength() {
        // given
        String message = "1234567890".repeat(40) + "c";

        // when, then
        assertThatThrownBy(() -> createGuestBookCardWithMessage(message))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("방명록 카드 메세지는 최대 400까지 입력 가능합니다.");
    }

    @DisplayName("메세지는 이모지를 한 글자로 간주해 400자까지 입력 가능하다")
    @ValueSource(strings = {"😀", "👦", "👨", "‍👩‍", "‍👦", "‍👩‍👧‍", "👨‍👩‍👧", "👨‍👩‍👧‍👦"})
    @ParameterizedTest
    void countEmojiAsOneCharAtMessage(String emoji) {
        // given
        String message = emoji.repeat(399) + "c";

        // when, then
        assertThatCode(() -> createGuestBookCardWithMessage(message)).doesNotThrowAnyException();
    }

    @DisplayName("방명록 카드를 생성하면 미읽음 처리된다")
    @Test
    void defaultIsReadFalse() {
        // given
        GuestBookCard guestBookCard = createGuestBookCard();

        // then
        assertThat(guestBookCard.isRead()).isFalse();
    }

    @Nested
    class Read {

        @DisplayName("스페이스 호스트이면 노출 상태의 방명록 카드를 읽음 처리한다")
        @Test
        void isReadTrueAfterSpaceHostReadVisibleGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(VISIBLE);

            // when
            guestBookCard.read(true);

            // then
            assertThat(guestBookCard.isRead()).isTrue();
        }

        @DisplayName("스페이스 호스트이면 호스트 숨김 상태의 방명록 카드를 읽음 처리한다")
        @Test
        void isReadTrueAfterSpaceHostReadHostHiddenGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(HIDDEN_BY_HOST);

            // when
            guestBookCard.read(true);

            // then
            assertThat(guestBookCard.isRead()).isTrue();
        }

        @DisplayName("스페이스 호스트이면 관리자 숨김 상태의 방명록 카드를 읽지 못한다")
        @Test
        void throwExceptionWhenSpaceHostReadAdminHiddenGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(HIDDEN_BY_ADMIN);

            // when, then
            assertThatThrownBy(() -> guestBookCard.read(true))
                .isInstanceOf(GuestbookCardNotReadableException.class);
        }

        @DisplayName("스페이스 호스트가 아니면 호스트 숨김 상태의 방명록 카드를 읽지 못한다")
        @Test
        void throwExceptionWhenNotSpaceHostReadHostHiddenGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(HIDDEN_BY_HOST);

            // when, then
            assertThatThrownBy(() -> guestBookCard.read(false))
                .isInstanceOf(GuestbookCardNotReadableException.class);
        }

        @DisplayName("스페이스 호스트가 아니면 관리자 숨김 상태의 방명록 카드를 읽지 못한다")
        @Test
        void throwExceptionWhenNotSpaceHostReadAdminHiddenGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(HIDDEN_BY_ADMIN);

            // when, then
            assertThatThrownBy(() -> guestBookCard.read(false))
                .isInstanceOf(GuestbookCardNotReadableException.class);
        }

        @DisplayName("스페이스 호스트가 아니면 노출 상태의 방명록 카드를 읽을 수 있고 읽음 처리는 하지 않는다")
        @Test
        void canReadWhenNotSpaceHostReadVisibleGuestbookCard() {
            // given
            GuestBookCard guestBookCard = createWithVisibilityStatus(VISIBLE);

            // when, then
            assertAll(
                () -> assertThatCode(() -> guestBookCard.read(false))
                    .doesNotThrowAnyException(),
                () -> assertThat(guestBookCard.isRead()).isFalse()
            );
        }
    }


    @DisplayName("관리자에 의해 숨김 처리된다")
    @Test
    void hideByAdmin() {
        // given
        GuestBookCard guestBookCard = createGuestBookCard();

        // when
        guestBookCard.hideByAdmin();

        // then
        assertThat(guestBookCard.getVisibilityStatus()).isEqualTo(VisibilityStatus.HIDDEN_BY_ADMIN);
    }
}
