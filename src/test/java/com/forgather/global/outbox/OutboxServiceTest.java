package com.forgather.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class OutboxServiceTest extends TestOnContainer {

    private final OutboxService outboxService;
    private final OutboxRepository outboxRepository;

    @Autowired
    public OutboxServiceTest(OutboxService outboxService, OutboxRepository outboxRepository) {
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
    }

    private record TestPayload(Long hostId, String value) {
    }

    private Outbox savePending(String payload) {
        return outboxRepository.save(Outbox.pending(OutboxType.SOCIAL_REVOKE, payload));
    }

    @DisplayName("payload를 JSON 문자열로 직렬화해 PENDING 상태로 저장한다")
    @Test
    void savePendingWithSerializedPayload() {
        // given
        TestPayload payload = new TestPayload(1L, "value");

        // when
        Outbox saved = outboxService.save(OutboxType.SOCIAL_REVOKE, payload);

        // then
        Outbox found = outboxRepository.getByIdOrThrow(saved.getId());
        assertAll(
            () -> assertThat(found.getType()).isEqualTo(OutboxType.SOCIAL_REVOKE),
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(found.getPayload()).isEqualTo("{\"hostId\":1,\"value\":\"value\"}"),
            () -> assertThat(found.getFailCount()).isZero()
        );
    }

    @DisplayName("타입이 일치하는 PENDING 상태의 outbox만 조회한다")
    @Test
    void findPendingTasksOnlyPending() {
        // given
        Outbox pending = savePending("pending");
        Outbox completed = savePending("completed");
        outboxService.complete(completed.getId());

        // when
        List<Outbox> found = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);

        // then
        assertThat(found).extracting(Outbox::getId)
            .containsExactly(pending.getId());
    }

    @DisplayName("완료 처리하면 상태가 COMPLETED로 바뀌고 payload는 비워진다")
    @Test
    void completeMarksCompletedAndClearsPayload() {
        // given
        Outbox outbox = savePending("payload");

        // when
        outboxService.complete(outbox.getId());

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.COMPLETED),
            () -> assertThat(found.getPayload()).isNull()
        );
    }

    @DisplayName("실패 처리하면 상태가 FAILED로 바뀌고 실패 횟수가 1 증가하며 payload는 유지된다")
    @Test
    void failMarksFailedAndKeepsPayload() {
        // given
        Outbox outbox = savePending("payload");

        // when
        outboxService.fail(outbox.getId());

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.FAILED),
            () -> assertThat(found.getFailCount()).isEqualTo(1),
            () -> assertThat(found.getPayload()).isEqualTo("payload")
        );
    }

    @DisplayName("존재하지 않는 outbox를 실패 처리하면 예외가 발생한다")
    @Test
    void failNotFound() {
        // when & then
        assertThatThrownBy(() -> outboxService.fail(Long.MAX_VALUE))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("실패 횟수를 호출한 횟수만큼 증가시키고 상태는 바꾸지 않는다")
    @Test
    void increaseFailCountOnlyIncrements() {
        // given
        Outbox outbox = savePending("payload");

        // when
        outboxService.increaseFailCount(outbox.getId());
        outboxService.increaseFailCount(outbox.getId());

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(found.getFailCount()).isEqualTo(2)
        );
    }

    @DisplayName("실패 횟수가 상한 이상인 PENDING outbox를 FAILED로 전환하고 payload는 유지한다")
    @Test
    void failExhaustedMarksFailedAndKeepsPayload() {
        // given
        Outbox outbox = savePending("payload");
        outboxService.increaseFailCount(outbox.getId());
        outboxService.increaseFailCount(outbox.getId());

        // when
        int failed = outboxService.failExhausted(OutboxType.SOCIAL_REVOKE, 2);

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(failed).isEqualTo(1),
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.FAILED),
            () -> assertThat(found.getPayload()).isEqualTo("payload"),
            () -> assertThat(found.getFailCount()).isEqualTo(2)
        );
    }

    @DisplayName("실패 횟수가 상한 미만이거나 PENDING이 아닌 outbox는 FAILED로 전환하지 않는다")
    @Test
    void failExhaustedSkipsBelowThresholdAndNonPending() {
        // given
        Outbox belowThreshold = savePending("below");
        outboxService.increaseFailCount(belowThreshold.getId());
        Outbox completed = savePending("completed");
        outboxService.increaseFailCount(completed.getId());
        outboxService.increaseFailCount(completed.getId());
        outboxService.complete(completed.getId());

        // when
        int failed = outboxService.failExhausted(OutboxType.SOCIAL_REVOKE, 2);

        // then
        assertAll(
            () -> assertThat(failed).isZero(),
            () -> assertThat(outboxRepository.getByIdOrThrow(belowThreshold.getId()).getStatus())
                .isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(outboxRepository.getByIdOrThrow(completed.getId()).getStatus())
                .isEqualTo(OutboxStatus.COMPLETED)
        );
    }

    @DisplayName("PENDING outbox를 취소 처리하면 상태가 CANCELED로 바뀌고 payload는 비워진다")
    @Test
    void cancelMarksCanceledAndClearsPayload() {
        // given
        Outbox outbox = savePending("payload");

        // when
        boolean canceled = outboxService.cancel(outbox.getId());

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(canceled).isTrue(),
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.CANCELED),
            () -> assertThat(found.getPayload()).isNull()
        );
    }

    @DisplayName("PENDING이 아닌 outbox는 취소 처리하지 않는다")
    @Test
    void cancelSkipsNonPending() {
        // given
        Outbox completed = savePending("completed");
        outboxService.complete(completed.getId());

        // when
        boolean canceled = outboxService.cancel(completed.getId());

        // then
        assertAll(
            () -> assertThat(canceled).isFalse(),
            () -> assertThat(outboxRepository.getByIdOrThrow(completed.getId()).getStatus())
                .isEqualTo(OutboxStatus.COMPLETED)
        );
    }

    @DisplayName("취소된 outbox는 PENDING 조회 대상에서 제외된다")
    @Test
    void findPendingTasksExcludesCanceled() {
        // given
        Outbox canceled = savePending("canceled");
        outboxService.cancel(canceled.getId());

        // when
        List<Outbox> found = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);

        // then
        assertThat(found).extracting(Outbox::getId).doesNotContain(canceled.getId());
    }

    @DisplayName("존재하지 않는 outbox를 취소 처리하면 예외 없이 false를 반환한다")
    @Test
    void cancelNotFound() {
        // when & then
        assertThat(outboxService.cancel(Long.MAX_VALUE)).isFalse();
    }

    @DisplayName("존재하지 않는 outbox를 완료 처리하면 예외가 발생한다")
    @Test
    void completeNotFound() {
        // when & then
        assertThatThrownBy(() -> outboxService.complete(Long.MAX_VALUE))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("존재하지 않는 outbox의 실패 횟수를 증가시키면 예외 없이 아무 일도 일어나지 않는다")
    @Test
    void increaseFailCountNotFound() {
        // when & then
        assertThatCode(() -> outboxService.increaseFailCount(Long.MAX_VALUE))
            .doesNotThrowAnyException();
    }

    @DisplayName("outbox id가 null이면 예외가 발생한다")
    @Test
    void completeNullId() {
        // when & then
        assertThatThrownBy(() -> outboxService.complete(null))
            .isInstanceOf(BaseNullPointerException.class);
    }
}
