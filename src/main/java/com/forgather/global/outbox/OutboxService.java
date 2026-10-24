package com.forgather.global.outbox;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxService {

    private final List<OutboxHandler> outboxHandlers;
    private final OutboxStateService outboxStateService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Outbox save(OutboxType type, Object payload) {
        Outbox outbox = Outbox.pending(type, serialize(payload));
        return outboxRepository.save(outbox);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BaseException("outbox payload 직렬화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OutboxProcessResult process() {
        int succeededCount = 0;
        int failedCount = 0;
        int canceledCount = 0;
        for (Outbox outbox : outboxStateService.findProcessable()) {
            OutboxHandler handler = findOutboxHandler(outbox);
            if (handler == null) {
                log.error("처리 가능한 핸들러가 없습니다. outboxId: {}, type: {}", outbox.getId(), outbox.getType());
                continue;
            }

            try {
                if (!outboxStateService.processing(outbox.getId(), handler.getLeaseDurationSeconds())) {
                    continue;
                }
            } catch (Exception e) {
                log.error("outbox 선점 실패. outboxId: {}", outbox.getId(), e);
                continue;
            }

            OutboxTaskResult result;
            try {
                result = handler.process(outbox);
            } catch (Exception e) {
                log.warn("outbox 처리 실패. outboxId: {}, type: {}", outbox.getId(), outbox.getType(), e);
                failedCount++;
                try {
                    if (!outboxStateService.retry(outbox.getId(), handler.getMaxFailCount())) {
                        log.warn("재시도할 PROCESSING outbox가 없습니다. outboxId: {}", outbox.getId());
                    }
                } catch (Exception stateException) {
                    log.error("outbox 실패 기록 중 오류가 발생했습니다. outboxId: {}", outbox.getId(), stateException);
                }
                continue;
            }

            try {
                boolean updated = switch (result) {
                    case COMPLETED -> outboxStateService.complete(outbox.getId());
                    case CANCELED -> outboxStateService.cancel(outbox.getId());
                    case FAILED -> outboxStateService.fail(outbox.getId());
                };
                if (!updated) {
                    log.warn("상태를 변경할 PROCESSING outbox가 없습니다. outboxId: {}", outbox.getId());
                    continue;
                }
                switch (result) {
                    case COMPLETED -> succeededCount++;
                    case CANCELED -> canceledCount++;
                    case FAILED -> failedCount++;
                }
            } catch (Exception e) {
                log.error("outbox 상태 변경 실패. outboxId: {}", outbox.getId(), e);
            }
        }
        return new OutboxProcessResult(succeededCount, failedCount, canceledCount);
    }

    private OutboxHandler findOutboxHandler(Outbox outbox) {
        return outboxHandlers.stream()
            .filter(candidate -> candidate.supports(outbox.getType()))
            .findFirst().orElse(null);
    }
}
