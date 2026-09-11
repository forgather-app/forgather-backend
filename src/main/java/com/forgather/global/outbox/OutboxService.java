package com.forgather.global.outbox;

import static com.forgather.global.outbox.OutboxStatus.PENDING;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public Outbox save(OutboxType type, Object payload) {
        Outbox outbox = Outbox.pending(type, payload);
        return outboxRepository.save(outbox);
    }

    public List<Outbox> findPendingTasks(OutboxType type) {
        return outboxRepository.findByTypeAndStatus(type, PENDING);
    }
}
