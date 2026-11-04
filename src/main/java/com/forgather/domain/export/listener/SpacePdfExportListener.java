package com.forgather.domain.export.listener;

import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.forgather.domain.export.event.SpacePdfExportRequested;
import com.forgather.global.outbox.OutboxService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SpacePdfExportListener {

    private final TaskExecutor taskExecutor;
    private final OutboxService outboxService;

    public SpacePdfExportListener(@Qualifier("taskExecutor") TaskExecutor taskExecutor, OutboxService outboxService) {
        this.taskExecutor = taskExecutor;
        this.outboxService = outboxService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(SpacePdfExportRequested event) {
        try {
            taskExecutor.execute(() -> outboxService.process(event.outboxId()));
        } catch (RejectedExecutionException e) {
            log.warn("PDF 추출 작업 제출 거절. 스케줄러에서 재시도합니다. outboxId: {}", event.outboxId(), e);
        }
    }
}
