package com.forgather.global.auth.model;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HostTest {

    @DisplayName("익명화하면 개인정보가 제거되고 익명화 시각이 기록된다")
    @Test
    void anonymize() {
        // given
        Host host = createHost();

        // when
        host.anonymize();

        // then
        assertAll(
            () -> assertThat(host.getName()).isEqualTo("탈퇴한 회원"),
            () -> assertThat(host.getNickname()).isNull(),
            () -> assertThat(host.getPictureUrl()).isNull(),
            () -> assertThat(host.getEmail()).isNull(),
            () -> assertThat(host.getAnonymizedAt()).isNotNull()
        );
    }

    @DisplayName("이미 익명화된 회원을 다시 익명화해도 익명화 시각이 변경되지 않는다")
    @Test
    void anonymizeIsIdempotent() {
        // given
        Host host = createHost();
        host.anonymize();
        LocalDateTime firstAnonymizedAt = host.getAnonymizedAt();

        // when
        host.anonymize();

        // then
        assertThat(host.getAnonymizedAt()).isEqualTo(firstAnonymizedAt);
    }
}
