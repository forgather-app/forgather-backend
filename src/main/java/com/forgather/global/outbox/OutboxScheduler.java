package com.forgather.global.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;

    @Scheduled(cron = "0 */1 * * * *")
    public void processOutbox() {
        try {
            OutboxProcessResult result = outboxService.process();
            if (!result.isEmpty()) {
                log.info("outbox 처리 완료. 성공: {}건, 실패: {}건, 취소: {}건",
                    result.succeededCount(), result.failedCount(), result.canceledCount());
            }
        } catch (Exception e) {
            log.error("outbox 배치 실패", e);
        }
    }
}
