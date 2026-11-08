package com.forgather.global.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<String> processing(Long id, long leaseDurationSeconds) {
        if (leaseDurationSeconds <= 0) {
            throw new IllegalArgumentException("선점 시간은 양수여야 합니다. leaseDurationSeconds: " + leaseDurationSeconds);
        }
        String claimToken = UUID.randomUUID().toString();
        return outboxRepository.claim(id, leaseDurationSeconds, claimToken) == 1
            ? Optional.of(claimToken) : Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(Long id, String claimToken) {
        return outboxRepository.finishProcessing(id, claimToken, OutboxStatus.COMPLETED) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancel(Long id, String claimToken) {
        return outboxRepository.finishProcessing(id, claimToken, OutboxStatus.CANCELED) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fail(Long id, String claimToken) {
        return outboxRepository.recordFailure(id, claimToken, false, 1) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retry(Long id, String claimToken, int maxFailCount) {
        return outboxRepository.recordFailure(id, claimToken, true, maxFailCount) == 1;
    }
}
