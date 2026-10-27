package com.forgather.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.NotificationType;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseNullPointerException;

class NotificationCommandTest {

    @DisplayName("수신 호스트가 null이면 알림 생성 요청을 만들 수 없다")
    @Test
    void rejectNullRecipient() {
        // when & then
        assertThatThrownBy(() -> new NotificationCommand(null, NotificationType.PLACEHOLDER, null, null, List.of(1)))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 수신 호스트는 null일 수 없습니다.");
    }

    @DisplayName("알림 종류가 null이면 알림 생성 요청을 만들 수 없다")
    @Test
    void rejectNullType() {
        // given
        Host host = HostFixture.createHost();

        // when & then
        assertThatThrownBy(() -> new NotificationCommand(host, null, null, null, List.of(1)))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 종류는 null일 수 없습니다.");
    }

    @DisplayName("템플릿 인자를 null로 넘기면 빈 리스트로 보관한다")
    @Test
    void defaultTemplateArgsToEmptyList() {
        // given
        Host host = HostFixture.createHost();

        // when
        NotificationCommand command = new NotificationCommand(host, NotificationType.PLACEHOLDER, null, null, null);

        // then
        assertThat(command.templateArgs()).isEmpty();
    }
}
