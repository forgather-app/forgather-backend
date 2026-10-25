package com.forgather.domain.notification.model;

import static com.forgather.fixture.NotificationFixture.SPACE_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class NotificationTargetTest {

    @DisplayName("스페이스 코드로 스페이스 대상을 만든다")
    @Test
    void createSpaceTarget() {
        // when
        NotificationTarget target = NotificationTarget.space(SPACE_CODE);

        // then
        assertAll(
            () -> assertThat(target.type()).isEqualTo(NotificationTargetType.SPACE),
            () -> assertThat(target.key()).isEqualTo(SPACE_CODE)
        );
    }

    @DisplayName("대상 종류가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenTypeIsNull() {
        // when & then
        assertThatThrownBy(() -> new NotificationTarget(null, SPACE_CODE))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 대상 종류는 null일 수 없습니다.");
    }

    @DisplayName("대상 키가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenKeyIsNull() {
        // when & then
        assertThatThrownBy(() -> NotificationTarget.space(null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 대상 키는 null일 수 없습니다.");
    }

    @DisplayName("대상 키가 공백만이면 서버 오류로 분류한 예외를 던진다")
    @Test
    void throwExceptionWhenKeyIsBlank() {
        // when & then
        assertThatThrownBy(() -> NotificationTarget.space("   "))
            .isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(500))
            .hasMessageContaining("알림 대상 키는 공백만 입력할 수 없습니다.");
    }

    @DisplayName("대상 키는 255자까지 허용한다")
    @Test
    void allowKeyAtMaxLength() {
        // given
        String key = "a".repeat(255);

        // when & then
        assertThatCode(() -> NotificationTarget.space(key)).doesNotThrowAnyException();
    }

    @DisplayName("대상 키가 255자를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenKeyExceedsMaxLength() {
        // given
        String key = "a".repeat(256);

        // when & then
        assertThatThrownBy(() -> NotificationTarget.space(key))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("알림 대상 키는 최대 255자까지 가능합니다.");
    }
}
