package com.forgather.domain.notification.model;

import static com.forgather.fixture.NotificationFixture.SPACE_CODE;
import static com.forgather.fixture.NotificationFixture.createNotification;
import static com.forgather.fixture.NotificationFixture.createNotificationWithDedupeKey;
import static com.forgather.fixture.NotificationFixture.createWithTitleAndBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.domain.host.model.Host;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class NotificationTest {

    private static final String TITLE = "제목";
    private static final String BODY = "본문";

    @DisplayName("종류의 템플릿으로 제목과 본문을 렌더링해 생성한다")
    @Test
    void createWithRenderedTemplate() {
        // given
        Host host = HostFixture.createHost();

        // when
        Notification notification = Notification.of(
            host,
            NotificationType.PLACEHOLDER,
            NotificationTarget.space(SPACE_CODE),
            "dedupe-key",
            3
        );

        // then
        assertAll(
            () -> assertThat(notification.getHost()).isSameAs(host),
            () -> assertThat(notification.getType()).isEqualTo(NotificationType.PLACEHOLDER),
            () -> assertThat(notification.getTitle()).isEqualTo("임시 알림"),
            () -> assertThat(notification.getBody()).isEqualTo("임시 알림 본문 3"),
            () -> assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.SPACE),
            () -> assertThat(notification.getTargetKey()).isEqualTo(SPACE_CODE),
            () -> assertThat(notification.hasTarget()).isTrue(),
            () -> assertThat(notification.getDedupeKey()).isEqualTo("dedupe-key"),
            () -> assertThat(notification.isSent()).isFalse(),
            () -> assertThat(notification.isRead()).isFalse(),
            () -> assertThat(notification.getDeletedAt()).isNull()
        );
    }

    @DisplayName("대상과 중복 방지 키 없이 생성할 수 있다")
    @Test
    void createWithoutTargetAndDedupeKey() {
        // when
        Notification notification = Notification.of(
            HostFixture.createHost(),
            NotificationType.PLACEHOLDER,
            null,
            null,
            3
        );

        // then
        assertAll(
            () -> assertThat(notification.hasTarget()).isFalse(),
            () -> assertThat(notification.getTargetType()).isNull(),
            () -> assertThat(notification.getTargetKey()).isNull(),
            () -> assertThat(notification.getDedupeKey()).isNull()
        );
    }

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("수신 호스트가 null이면 생성할 수 없다")
        @Test
        void createWithNullHost() {
            // when & then
            assertThatThrownBy(() -> NotificationFixture.createNotificationWithHost(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("알림 수신 호스트는 null일 수 없습니다.");
        }

        @DisplayName("종류가 null이면 생성할 수 없다")
        @Test
        void createWithNullType() {
            // when & then
            assertThatThrownBy(() -> Notification.of(HostFixture.createHost(), null, null, null, 3))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("알림 종류는 null일 수 없습니다.");
        }

        @DisplayName("제목이 null이면 생성할 수 없다")
        @Test
        void createWithNullTitle() {
            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody(null, BODY))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("알림 제목은 null일 수 없습니다.");
        }

        @DisplayName("본문이 null이면 생성할 수 없다")
        @Test
        void createWithNullBody() {
            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody(TITLE, null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("알림 본문은 null일 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("제목·본문 검증")
    class TitleAndBody {

        @DisplayName("제목이 공백만이면 예외를 던진다")
        @Test
        void throwExceptionWhenTitleIsBlank() {
            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody("   ", BODY))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("알림 제목은 공백만 입력할 수 없습니다.");
        }

        @DisplayName("제목은 100자까지 허용한다")
        @Test
        void allowTitleAtMaxLength() {
            // given
            String title = "가".repeat(100);

            // when & then
            assertThatCode(() -> createWithTitleAndBody(title, BODY)).doesNotThrowAnyException();
        }

        @DisplayName("제목이 100자를 초과하면 서버 오류로 분류한 예외를 던진다")
        @Test
        void throwExceptionWhenTitleExceedsMaxLength() {
            // given
            String title = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody(title, BODY))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                    assertThat(exception.getStatusCode()).isEqualTo(500))
                .hasMessageContaining("알림 제목은 최대 100자까지 가능합니다.");
        }

        @DisplayName("본문이 공백만이면 예외를 던진다")
        @Test
        void throwExceptionWhenBodyIsBlank() {
            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody(TITLE, "   "))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("알림 본문은 공백만 입력할 수 없습니다.");
        }

        @DisplayName("본문은 이모지를 한 글자로 간주해 500자까지 허용한다")
        @Test
        void allowBodyAtMaxLengthWithEmoji() {
            // given
            String body = "👨‍👩‍👧‍👦".repeat(500);

            // when & then
            assertThatCode(() -> createWithTitleAndBody(TITLE, body)).doesNotThrowAnyException();
        }

        @DisplayName("본문이 500자를 초과하면 예외를 던진다")
        @Test
        void throwExceptionWhenBodyExceedsMaxLength() {
            // given
            String body = "가".repeat(501);

            // when & then
            assertThatThrownBy(() -> createWithTitleAndBody(TITLE, body))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("알림 본문은 최대 500자까지 가능합니다.");
        }
    }

    @Nested
    @DisplayName("중복 방지 키 검증")
    class DedupeKey {

        @DisplayName("중복 방지 키가 공백만이면 서버 오류로 분류한 예외를 던진다")
        @Test
        void throwExceptionWhenDedupeKeyIsBlank() {
            // when & then
            assertThatThrownBy(() -> createNotificationWithDedupeKey("   "))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                    assertThat(exception.getStatusCode()).isEqualTo(500))
                .hasMessageContaining("알림 중복 방지 키는 공백만 입력할 수 없습니다.");
        }

        @DisplayName("중복 방지 키는 255자까지 허용한다")
        @Test
        void allowDedupeKeyAtMaxLength() {
            // given
            String dedupeKey = "a".repeat(255);

            // when & then
            assertThatCode(() -> createNotificationWithDedupeKey(dedupeKey)).doesNotThrowAnyException();
        }

        @DisplayName("중복 방지 키가 255자를 초과하면 예외를 던진다")
        @Test
        void throwExceptionWhenDedupeKeyExceedsMaxLength() {
            // given
            String dedupeKey = "a".repeat(256);

            // when & then
            assertThatThrownBy(() -> createNotificationWithDedupeKey(dedupeKey))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("알림 중복 방지 키는 최대 255자까지 가능합니다.");
        }
    }

    @Nested
    @DisplayName("전송·읽음 상태")
    class SentAndRead {

        @DisplayName("전송 접수 시각을 기록하면 전송됨 상태가 된다")
        @Test
        void markSent() {
            // given
            Notification notification = createNotification();
            LocalDateTime sentAt = LocalDateTime.of(2026, 10, 21, 9, 0);

            // when
            notification.markSent(sentAt);

            // then
            assertAll(
                () -> assertThat(notification.isSent()).isTrue(),
                () -> assertThat(notification.getSentAt()).isEqualTo(sentAt)
            );
        }

        @DisplayName("전송 접수 시각이 이미 있으면 처음 값을 유지한다")
        @Test
        void keepFirstSentAt() {
            // given
            Notification notification = createNotification();
            LocalDateTime first = LocalDateTime.of(2026, 10, 21, 9, 0);
            LocalDateTime second = first.plusMinutes(5);
            notification.markSent(first);

            // when
            notification.markSent(second);

            // then
            assertThat(notification.getSentAt()).isEqualTo(first);
        }

        @DisplayName("전송 접수 시각이 null이면 예외를 던진다")
        @Test
        void throwExceptionWhenSentAtIsNull() {
            // given
            Notification notification = createNotification();

            // when & then
            assertThatThrownBy(() -> notification.markSent(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("전송 접수 시각은 null일 수 없습니다.");
        }

        @DisplayName("읽으면 읽음 시각이 기록되고 다시 읽어도 처음 값을 유지한다")
        @Test
        void readIsIdempotent() {
            // given
            Notification notification = createNotification();

            // when
            notification.read();
            LocalDateTime firstReadAt = notification.getReadAt();
            notification.read();

            // then
            assertAll(
                () -> assertThat(notification.isRead()).isTrue(),
                () -> assertThat(firstReadAt).isNotNull(),
                () -> assertThat(notification.getReadAt()).isEqualTo(firstReadAt)
            );
        }
    }
}
