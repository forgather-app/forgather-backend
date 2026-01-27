package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.util.RandomCodeGenerator;

class AdminSessionTest {

    private RandomCodeGenerator randomCodeGenerator = new RandomCodeGenerator();

    @DisplayName("어드민 세션을 생성한다.")
    @Test
    void createAdminSession() {
        // given
        Long adminUserId = 1L;
        String username = "admin";

        // when
        AdminSession session = AdminSession.create(adminUserId, username, randomCodeGenerator);

        // then
        assertAll(
            () -> assertThat(session.getSessionId()).isNotNull(),
            () -> assertThat(session.getAdminUserId()).isEqualTo(adminUserId),
            () -> assertThat(session.getUsername()).isEqualTo(username),
            () -> assertThat(session.getCreatedAt()).isNotNull(),
            () -> assertThat(session.getLastAccessedAt()).isEqualTo(session.getCreatedAt()),
            () -> assertThat(session.getMaxInactiveIntervalSeconds()).isEqualTo(30 * 60)
        );
    }

    @DisplayName("세션의 만료 시간은 30분이다.")
    @Test
    void isNotExpiredWhenCreated() {
        // given
        AdminSession session = AdminSession.create(1L, "admin", randomCodeGenerator);
        LocalDateTime sessionCreateTime = LocalDateTime.now().plusMinutes(29);
        // when & then
        assertThat(session.isExpired(sessionCreateTime)).isFalse();
    }

    @DisplayName("마지막 접근 시간이 만료 시간을 초과하면 세션이 만료된다.")
    @Test
    void isExpiredAfterTimeout() {
        // given
        AdminSession session = AdminSession.create(1L, "username", randomCodeGenerator);
        LocalDateTime expiredTime = LocalDateTime.now().plusMinutes(30);

        // when & then
        assertThat(session.isExpired(expiredTime)).isTrue();
    }

    @DisplayName("마지막 접근 시간을 갱신하면 새로운 세션 객체가 반환된다.")
    @Test
    void refresh() {
        // given
        AdminSession session = AdminSession.create(1L, "admin", randomCodeGenerator);
        LocalDateTime originalLastAccessedAt = session.getLastAccessedAt();

        // when
        AdminSession updatedSession = session.refresh();

        // then
        assertAll(
            () -> assertThat(updatedSession.getSessionId()).isEqualTo(session.getSessionId()),
            () -> assertThat(updatedSession.getAdminUserId()).isEqualTo(session.getAdminUserId()),
            () -> assertThat(updatedSession.getUsername()).isEqualTo(session.getUsername()),
            () -> assertThat(updatedSession.getCreatedAt()).isEqualTo(session.getCreatedAt()),
            () -> assertThat(updatedSession.getLastAccessedAt()).isAfterOrEqualTo(originalLastAccessedAt)
        );
    }
}
