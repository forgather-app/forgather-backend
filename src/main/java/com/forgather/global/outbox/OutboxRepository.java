package com.forgather.global.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByTypeAndStatus(OutboxType type, OutboxStatus outboxStatus);
}
