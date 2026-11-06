package com.forgather.domain.notification.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;

class NotificationTypeTest {

    @DisplayName("템플릿 인자로 제목과 본문을 렌더링한다")
    @Test
    void renderWithArguments() {
        // when
        String title = NotificationType.PLACEHOLDER.renderTitle(3);
        String body = NotificationType.PLACEHOLDER.renderBody(3);

        // then
        assertAll(
            () -> assertThat(title).isEqualTo("임시 알림"),
            () -> assertThat(body).isEqualTo("임시 알림 본문 3")
        );
    }

    @DisplayName("템플릿 인자가 누락되면 서버 오류로 분류한 예외를 던진다")
    @Test
    void throwExceptionWhenArgumentIsMissing() {
        // when & then
        assertThatThrownBy(() -> NotificationType.PLACEHOLDER.renderBody())
            .isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(500))
            .hasMessageContaining("알림 템플릿 인자가 올바르지 않습니다.");
    }

    @DisplayName("템플릿 인자가 null이면 \"null\"로 렌더링하지 않고 예외를 던진다")
    @Test
    void throwExceptionWhenArgumentIsNull() {
        // when & then
        assertThatThrownBy(() -> NotificationType.PLACEHOLDER.renderBody((Object)null))
            .isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(500))
            .hasMessageContaining("알림 템플릿 인자는 null일 수 없습니다.");
    }

    @DisplayName("템플릿 인자 배열 자체가 null이면 예외를 던진다")
    @Test
    void throwExceptionWhenArgumentArrayIsNull() {
        // when & then
        assertThatThrownBy(() -> NotificationType.PLACEHOLDER.renderBody((Object[])null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("알림 템플릿 인자는 null일 수 없습니다.");
    }

    @DisplayName("템플릿 인자 타입이 맞지 않으면 종류 이름을 포함한 예외를 던진다")
    @Test
    void throwExceptionWhenArgumentTypeMismatches() {
        // when & then
        assertThatThrownBy(() -> NotificationType.PLACEHOLDER.renderBody("셋"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("PLACEHOLDER");
    }
}
