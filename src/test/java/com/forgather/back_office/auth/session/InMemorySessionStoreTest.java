package com.forgather.back_office.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.SessionId;
import com.forgather.global.exception.NotFoundException;
import com.forgather.global.util.RandomCodeGenerator;

class InMemorySessionStoreTest {

    private InMemorySessionStore sessionStore;
    private RandomCodeGenerator randomCodeGenerator;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemorySessionStore();
        randomCodeGenerator = new RandomCodeGenerator();
    }

    @DisplayName("인메모리 기반의 세션 저장소에 어드민 세션 정보를 저장한다.")
    @Test
    void saveAdminSession() {
        // given
        AdminSession adminSession = AdminSession.create(1L, "username", randomCodeGenerator);

        // when & then
        assertThatCode(() -> sessionStore.save(adminSession))
            .doesNotThrowAnyException();
    }

    @DisplayName("저장된 세션을 세션 ID로 조회한다.")
    @Test
    void findBySessionId() {
        // given
        AdminSession adminSession = AdminSession.create(1L, "username", randomCodeGenerator);
        sessionStore.save(adminSession);

        // when
        AdminSession result = sessionStore.getBySessionId(adminSession.getSessionId());

        // then
        assertAll(
            () -> assertThat(result.getAdminUserId()).isEqualTo(1L),
            () -> assertThat(result.getUsername()).isEqualTo("username")
        );
    }

    @DisplayName("존재하지 않는 세션 ID로 조회하면 예외가 발생한다.")
    @Test
    void findBySessionIdNotFound() {
        // when & then
        assertThatThrownBy(() -> sessionStore.getBySessionId(SessionId.generate(randomCodeGenerator)))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("세션을 삭제하면 더 이상 조회되지 않는다.")
    @Test
    void deleteSession() {
        // given
        AdminSession adminSession = AdminSession.create(1L, "username", randomCodeGenerator);
        sessionStore.save(adminSession);
        SessionId sessionId = adminSession.getSessionId();

        // when
        sessionStore.delete(sessionId);

        // then
        assertThatThrownBy(() -> sessionStore.getBySessionId(sessionId))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("기준 시간을 두고 만료된 세션을 삭제한다.")
    @Test
    void deleteExpiredSessions() {
        // given
        AdminSession adminSession1 = AdminSession.create(1L, "username1", randomCodeGenerator);
        AdminSession adminSession2 = AdminSession.create(2L, "username2", randomCodeGenerator);
        sessionStore.save(adminSession1);
        sessionStore.save(adminSession2);
        LocalDateTime expiredDateTime = adminSession2.getCreatedAt().plusMinutes(31);

        // when & then
        assertThat(sessionStore.deleteExpiredSessions(expiredDateTime)).isEqualTo(2);
    }
}
