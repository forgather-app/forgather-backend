package com.forgather.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.repository.NotificationRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationFixture;
import com.forgather.global.exception.ConflictException;

/**
 * 호출자 트랜잭션에 참여한 상태에서 dedupeKey 중복이 호출자 트랜잭션의 커밋에 미치는 영향을 검증한다.
 * 테스트 자체를 @Transactional로 감싸면 커밋 결과를 볼 수 없어 TransactionTemplate으로 직접 트랜잭션을 연다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationServiceTransactionTest extends TestOnContainer {

    private static final String DEDUPE_KEY = "digest:abc:2026-10-25";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 스파이가 리포지토리 빈을 인터페이스 타입으로 감싸므로, 행 수는 JPA 구현 타입 대신 SQL로 센다.
     */
    private long countNotifications() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification", Long.class);
        return count == null ? 0 : count;
    }

    @MockitoSpyBean
    private NotificationRepository notificationRepository;

    @DisplayName("선조회로 걸러진 중복은 호출자가 예외를 잡고 계속 진행해도 트랜잭션이 정상 커밋된다")
    @Test
    void callerCanContinueAfterDuplicateDetectedByPreCheck() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        AtomicBoolean conflicted = new AtomicBoolean(false);

        // when
        assertThatCode(() -> transactionTemplate.executeWithoutResult(status -> {
            notificationService.create(NotificationFixture.createCommandWithHostAndDedupeKey(host, DEDUPE_KEY));
            try {
                notificationService.create(NotificationFixture.createCommandWithHostAndDedupeKey(host, DEDUPE_KEY));
            } catch (ConflictException e) {
                conflicted.set(true);
            }
            assertThat(status.isRollbackOnly()).isFalse();
        })).doesNotThrowAnyException();

        // then
        assertAll(
            () -> assertThat(conflicted).isTrue(),
            () -> assertThat(countNotifications()).isEqualTo(1)
        );
    }

    @DisplayName("선조회를 통과한 뒤 UNIQUE 위반이 나면 호출자가 예외를 잡아도 트랜잭션이 커밋되지 않는다")
    @Test
    void uniqueViolationMarksCallerTransactionRollbackOnly() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        // 선조회를 무력화해 두 번째 저장이 UNIQUE 위반으로 떨어지게 한다 (동시 생성 경합 재현)
        Mockito.doReturn(false).when(notificationRepository).existsByDedupeKey(DEDUPE_KEY);

        // when & then
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            notificationService.create(NotificationFixture.createCommandWithHostAndDedupeKey(host, DEDUPE_KEY));
            assertThatThrownBy(() -> notificationService.create(
                NotificationFixture.createCommandWithHostAndDedupeKey(host, DEDUPE_KEY)))
                .isInstanceOf(ConflictException.class);
            assertThat(status.isRollbackOnly()).isTrue();
        })).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(countNotifications()).isZero();
    }
}
