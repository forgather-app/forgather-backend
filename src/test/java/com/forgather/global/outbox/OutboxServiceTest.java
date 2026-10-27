package com.forgather.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class OutboxServiceTest extends TestOnContainer {

    @Autowired private OutboxService outboxService;
    @Autowired private OutboxRepository outboxRepository;
    @MockitoBean private OutboxScheduler outboxScheduler;

    private record TestPayload(Long hostId, String value) {
    }

    @DisplayName("payload를 JSON 문자열로 직렬화해 PENDING 상태로 저장한다")
    @Test
    void savePendingWithSerializedPayload() {
        // given
        TestPayload payload = new TestPayload(1L, "value");

        // when
        Outbox saved = outboxService.save(OutboxType.SOCIAL_REVOKE, payload);

        // then
        Outbox found = outboxRepository.findById(saved.getId()).orElseThrow();
        assertAll(
            () -> assertThat(found.getType()).isEqualTo(OutboxType.SOCIAL_REVOKE),
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(found.getPayload()).isEqualTo("{\"hostId\":1,\"value\":\"value\"}"),
            () -> assertThat(found.getFailCount()).isZero(),
            () -> assertThat(found.getLeaseExpiresAt()).isNull()
        );
    }
}
