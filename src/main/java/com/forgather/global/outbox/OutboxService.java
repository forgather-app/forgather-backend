package com.forgather.global.outbox;

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
}
