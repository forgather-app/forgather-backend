package com.forgather.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.forgather.container.TestOnContainer;

@ActiveProfiles({"test", "outbox-state-test"})
@SpringBootTest(classes = OutboxStateServiceTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OutboxStateServiceTest extends TestOnContainer {

    @Profile("outbox-state-test")
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableJpaAuditing
    @EntityScan(basePackageClasses = Outbox.class)
    @EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
    @Import(OutboxStateService.class)
    static class Config {
    }

    @Autowired private OutboxStateService service;
    @Autowired private OutboxRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Outbox pending() {
        return repository.save(Outbox.pending(OutboxType.SOCIAL_REVOKE, "payload"));
    }

    private void expire(Long id) {
        jdbc.update("UPDATE outbox SET lease_expires_at = CURRENT_TIMESTAMP - INTERVAL 1 SECOND WHERE id = ?", id);
    }

    @DisplayName("선점하면 만료 시각을 저장하고 만료 전에는 다시 선점하지 않는다")
    @Test
    void claimsPendingWithLease() {
        // given
        Outbox task = pending();

        // when
        Optional<String> claimed = service.processing(task.getId(), 20);

        // then
        assertAll(
            () -> assertThat(claimed).isPresent(),
            () -> assertThat(repository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PROCESSING),
            () -> assertThat(service.processing(task.getId(), 20)).isEmpty()
        );
    }

    @DisplayName("두 작업자가 동시에 선점하면 한 작업자만 성공한다")
    @Test
    void onlyOneWorkerClaimsTask() throws Exception {
        // given
        Outbox task = pending();
        CountDownLatch start = new CountDownLatch(1);

        // when
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            Future<Optional<String>> first = workers.submit(() -> {
                start.await();
                return service.processing(task.getId(), 20);
            });
            Future<Optional<String>> second = workers.submit(() -> {
                start.await();
                return service.processing(task.getId(), 20);
            });
            start.countDown();

            // then
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                .filteredOn(Optional::isPresent).hasSize(1);
        }
    }

    @DisplayName("만료된 PROCESSING만 PENDING과 함께 조회하고 다시 선점할 수 있다")
    @Test
    void reclaimsOnlyExpiredProcessing() {
        // given
        Outbox pending = pending();
        Outbox expired = pending();
        Outbox active = pending();
        service.processing(expired.getId(), 20);
        service.processing(active.getId(), 20);
        expire(expired.getId());
        repository.save(new Outbox(OutboxType.SOCIAL_REVOKE, OutboxStatus.FAILED, "failed", 5));
        repository.save(new Outbox(OutboxType.SOCIAL_REVOKE, OutboxStatus.COMPLETED, null, 0));
        repository.save(new Outbox(OutboxType.SOCIAL_REVOKE, OutboxStatus.CANCELED, null, 0));

        // when
        List<Outbox> processable = service.findProcessable();

        // then
        assertAll(
            () -> assertThat(processable).extracting(Outbox::getId).containsExactly(pending.getId(), expired.getId()),
            () -> assertThat(service.processing(expired.getId(), 60)).isPresent(),
            () -> assertThat(service.processing(active.getId(), 20)).isEmpty()
        );
    }

    @DisplayName("재시도 실패는 PENDING으로 돌아가고 다섯 번째 실패는 FAILED로 끝난다")
    @Test
    void failsAtRetryLimit() {
        // given
        Outbox task = pending();

        // when & then
        for (int attempt = 1; attempt <= 5; attempt++) {
            String token = service.processing(task.getId(), 20).orElseThrow();
            assertThat(service.retry(task.getId(), token, 5)).isTrue();
            Outbox stored = repository.findById(task.getId()).orElseThrow();
            int expectedFailCount = attempt;
            assertAll(
                () -> assertThat(stored.getFailCount()).isEqualTo(expectedFailCount),
                () -> assertThat(stored.getPayload()).isEqualTo("payload"),
                () -> assertThat(stored.getClaimToken()).isNull(),
                () -> assertThat(stored.getLeaseExpiresAt()).isNull(),
                () -> assertThat(stored.getStatus()).isEqualTo(expectedFailCount < 5 ? OutboxStatus.PENDING : OutboxStatus.FAILED)
            );
        }
        assertThat(service.processing(task.getId(), 20)).isEmpty();
    }

    @DisplayName("복구할 수 없는 작업 실패는 즉시 FAILED로 전환하고 payload를 보존한다")
    @Test
    void failsPermanently() {
        // given
        Outbox task = pending();
        String token = service.processing(task.getId(), 20).orElseThrow();

        // when
        assertThat(service.fail(task.getId(), token)).isTrue();

        // then
        Outbox stored = repository.findById(task.getId()).orElseThrow();
        assertAll(
            () -> assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED),
            () -> assertThat(stored.getFailCount()).isEqualTo(1),
            () -> assertThat(stored.getPayload()).isEqualTo("payload"),
            () -> assertThat(stored.getClaimToken()).isNull(),
            () -> assertThat(stored.getLeaseExpiresAt()).isNull()
        );
    }

    @DisplayName("선점 시간이 양수가 아니면 작업을 선점하지 않는다")
    @Test
    void rejectsNonPositiveLease() {
        // given
        Outbox task = pending();

        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> service.processing(task.getId(), 0)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThat(repository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING)
        );
    }
    @DisplayName("재선점 후 이전 토큰의 결과는 거절하고 현재 토큰의 결과만 반영한다")
    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "CANCELED", "FAILED", "RETRY"})
    void rejectsPreviousWorkerAfterReclaim(String result) {
        Outbox task = pending();
        String firstToken = service.processing(task.getId(), 20).orElseThrow();
        expire(task.getId());
        String secondToken = service.processing(task.getId(), 20).orElseThrow();

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(finish(task.getId(), firstToken, result)).isFalse();
        assertThat(finish(task.getId(), null, result)).isFalse();
        Outbox processing = repository.findById(task.getId()).orElseThrow();
        assertAll(
            () -> assertThat(processing.getStatus()).isEqualTo(OutboxStatus.PROCESSING),
            () -> assertThat(processing.getFailCount()).isZero(),
            () -> assertThat(processing.getPayload()).isEqualTo("payload"),
            () -> assertThat(processing.getClaimToken()).isEqualTo(secondToken)
        );

        assertThat(finish(task.getId(), secondToken, result)).isTrue();
        Outbox finished = repository.findById(task.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(
            result.equals("RETRY") ? OutboxStatus.PENDING : OutboxStatus.valueOf(result));
        assertThat(finished.getClaimToken()).isNull();
        assertThat(finished.getLeaseExpiresAt()).isNull();
    }

    @DisplayName("선점이 만료돼도 재선점 전에는 기존 토큰으로 완료할 수 있다")
    @Test
    void completesExpiredClaimBeforeReclaim() {
        Outbox task = pending();
        String token = service.processing(task.getId(), 20).orElseThrow();
        expire(task.getId());

        assertThat(service.complete(task.getId(), token)).isTrue();
        assertThat(repository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    }

    private boolean finish(Long id, String token, String result) {
        return switch (result) {
            case "COMPLETED" -> service.complete(id, token);
            case "CANCELED" -> service.cancel(id, token);
            case "FAILED" -> service.fail(id, token);
            case "RETRY" -> service.retry(id, token, 5);
            default -> throw new IllegalArgumentException(result);
        };
    }

}
