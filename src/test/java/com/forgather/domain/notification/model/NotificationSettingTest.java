package com.forgather.domain.notification.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.domain.host.model.Host;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseNullPointerException;

class NotificationSettingTest {

    @DisplayName("호스트와 알림 수신 여부로 생성하면 두 값이 그대로 저장된다")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createSetting(boolean enabled) {
        // given
        Host host = HostFixture.createHost();

        // when
        NotificationSetting setting = new NotificationSetting(host, enabled);

        // then
        assertAll(
            () -> assertThat(setting.getHost()).isSameAs(host),
            () -> assertThat(setting.isEnabled()).isEqualTo(enabled)
        );
    }

    @DisplayName("호스트가 null이면 알림 설정을 생성할 수 없다")
    @Test
    void createWithNullHost() {
        // when & then
        assertThatThrownBy(() -> new NotificationSetting(null, true))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 설정의 호스트는 null일 수 없습니다.");
    }
}
