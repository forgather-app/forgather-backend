package com.forgather.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationFixture;

/**
 * 기능 트랜잭션과 알림 생성 트랜잭션의 분리를 검증한다.
 * 커밋 이후 동작을 봐야 하므로 테스트를 @Transactional로 감싸지 않고, TransactionTemplate으로 기능 트랜잭션을 연다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationRequestedEventListenerTest extends TestOnContainer {

    private static final String DEDUPE_KEY = "digest:abc:2026-11-11";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HostRepository hostRepository;

    private long countNotifications() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification", Long.class);
        return count == null ? 0 : count;
    }

    private Long notificationHostId() {
        return jdbcTemplate.queryForObject("SELECT host_id FROM notification", Long.class);
    }

    private boolean hostExists(Long hostId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM host WHERE id = ?", Long.class, hostId);
        return count != null && count == 1;
    }

    @DisplayName("기능 트랜잭션이 커밋된 뒤에만 알림이 생성된다")
    @Test
    void createNotificationAfterCommit() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NotificationRequestedEvent(NotificationFixture.createCommandWithHost(host)));
            assertThat(countNotifications()).isZero();
        });

        // then
        assertAll(
            () -> assertThat(countNotifications()).isEqualTo(1),
            () -> assertThat(notificationHostId()).isEqualTo(host.getId())
        );
    }

    @DisplayName("기능 트랜잭션이 롤백되면 알림이 생성되지 않는다")
    @Test
    void doNotCreateNotificationWhenRolledBack() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NotificationRequestedEvent(NotificationFixture.createCommandWithHost(host)));
            status.setRollbackOnly();
        });

        // then
        assertThat(countNotifications()).isZero();
    }

    @DisplayName("알림 생성에 실패해도 예외가 전파되지 않고 기능 트랜잭션의 변경은 커밋된다")
    @Test
    void keepFeatureCommitWhenNotificationCreationFails() {
        // given
        Host recipient = hostRepository.save(HostFixture.createHost());

        // when
        Long[] featureHostId = new Long[1];
        assertThatCode(() -> transactionTemplate.executeWithoutResult(status -> {
            featureHostId[0] = hostRepository.save(HostFixture.createHost()).getId();
            eventPublisher.publishEvent(
                new NotificationRequestedEvent(NotificationFixture.createInvalidTemplateCommandWithHost(recipient)));
        })).doesNotThrowAnyException();

        // then
        assertAll(
            () -> assertThat(hostExists(featureHostId[0])).isTrue(),
            () -> assertThat(countNotifications()).isZero()
        );
    }

    @DisplayName("같은 중복 방지 키로 다시 요청하면 예외 없이 건너뛰어 알림이 한 건만 남는다")
    @Test
    void skipDuplicateRequestWithSameDedupeKey() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        NotificationRequestedEvent event = new NotificationRequestedEvent(
            NotificationFixture.createCommandWithHostAndDedupeKey(host, DEDUPE_KEY));
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // when & then
        assertThatCode(() -> transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event)))
            .doesNotThrowAnyException();
        assertThat(countNotifications()).isEqualTo(1);
    }

    @DisplayName("트랜잭션 밖에서 발행하면 즉시 알림이 생성된다")
    @Test
    void createNotificationImmediatelyWithoutTransaction() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        eventPublisher.publishEvent(new NotificationRequestedEvent(NotificationFixture.createCommandWithHost(host)));

        // then
        assertThat(countNotifications()).isEqualTo(1);
    }

    @DisplayName("기능 트랜잭션에서 저장한 호스트를 수신자로 요청해도 커밋 뒤 알림이 생성된다")
    @Test
    void createNotificationForHostSavedInFeatureTransaction() {
        // given
        Long[] hostId = new Long[1];

        // when
        transactionTemplate.executeWithoutResult(status -> {
            Host host = hostRepository.save(HostFixture.createHost());
            hostId[0] = host.getId();
            eventPublisher.publishEvent(new NotificationRequestedEvent(NotificationFixture.createCommandWithHost(host)));
        });

        // then
        assertAll(
            () -> assertThat(countNotifications()).isEqualTo(1),
            () -> assertThat(notificationHostId()).isEqualTo(hostId[0])
        );
    }
}
