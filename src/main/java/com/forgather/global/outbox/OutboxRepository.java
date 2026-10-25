package com.forgather.global.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Query("""
        SELECT o FROM Outbox o
        WHERE o.status = com.forgather.global.outbox.OutboxStatus.PENDING
           OR (o.status = com.forgather.global.outbox.OutboxStatus.PROCESSING
               AND o.leaseExpiresAt <= local datetime)
        ORDER BY o.id
        """)
    List<Outbox> findProcessable();

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Outbox o
        SET o.status = com.forgather.global.outbox.OutboxStatus.PROCESSING,
            o.leaseExpiresAt = local datetime + :leaseDurationSeconds second,
            o.updatedAt = CURRENT_TIMESTAMP
        WHERE o.id = :id
          AND (o.status = com.forgather.global.outbox.OutboxStatus.PENDING
               OR (o.status = com.forgather.global.outbox.OutboxStatus.PROCESSING
                   AND o.leaseExpiresAt <= local datetime))
        """)
    int claim(@Param("id") Long id, @Param("leaseDurationSeconds") long leaseDurationSeconds);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Outbox o
        SET o.status = :status, o.payload = null, o.leaseExpiresAt = null, o.updatedAt = CURRENT_TIMESTAMP
        WHERE o.id = :id AND o.status = com.forgather.global.outbox.OutboxStatus.PROCESSING
        """)
    int finishProcessing(@Param("id") Long id, @Param("status") OutboxStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Outbox o
        SET o.status = CASE
                WHEN :retryable = true AND o.failCount + 1 < :maxFailCount
                THEN com.forgather.global.outbox.OutboxStatus.PENDING
                ELSE com.forgather.global.outbox.OutboxStatus.FAILED END,
            o.failCount = o.failCount + 1,
            o.leaseExpiresAt = null,
            o.updatedAt = CURRENT_TIMESTAMP
        WHERE o.id = :id AND o.status = com.forgather.global.outbox.OutboxStatus.PROCESSING
        """)
    int recordFailure(@Param("id") Long id, @Param("retryable") boolean retryable,
        @Param("maxFailCount") int maxFailCount);
}
