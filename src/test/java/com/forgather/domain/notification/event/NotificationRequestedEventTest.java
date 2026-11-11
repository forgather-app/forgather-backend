package com.forgather.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseNullPointerException;

class NotificationRequestedEventTest {

    @DisplayName("알림 생성 요청 내용이 null이면 이벤트를 만들 수 없다")
    @Test
    void rejectNullCommand() {
        // when & then
        assertThatThrownBy(() -> new NotificationRequestedEvent(null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 생성 요청은 null일 수 없습니다.");
    }
}
