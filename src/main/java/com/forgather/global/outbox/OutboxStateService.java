package com.forgather.global.outbox;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboxStateService {

    private final OutboxRepository outboxRepository;

    public List<Outbox> findProcessable() {
        return outboxRepository.findProcessable();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processing(Long id, long leaseDurationSeconds) {
        if (leaseDurationSeconds <= 0) {
            throw new IllegalArgumentException("선점 시간은 양수여야 합니다. leaseDurationSeconds: " + leaseDurationSeconds);
        }
        return outboxRepository.claim(id, leaseDurationSeconds) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(Long id) {
        return outboxRepository.finishProcessing(id, OutboxStatus.COMPLETED) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancel(Long id) {
        return outboxRepository.finishProcessing(id, OutboxStatus.CANCELED) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fail(Long id) {
        return outboxRepository.recordFailure(id, false, 1) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retry(Long id, int maxFailCount) {
        return outboxRepository.recordFailure(id, true, maxFailCount) == 1;
    }
}
