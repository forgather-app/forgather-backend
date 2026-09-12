package com.forgather.global.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByTypeAndStatus(OutboxType type, OutboxStatus outboxStatus);

    /**
     * 동시 처리 시 lost update를 막기 위해 DB에서 원자적으로 증가시킨다.
     * 벌크 UPDATE는 Auditing을 타지 않으므로 updatedAt을 직접 갱신한다.
     *
     * @return 갱신된 행 수. 존재하지 않으면 0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Outbox o
        SET o.failCount = o.failCount + 1, o.updatedAt = :now
        WHERE o.id = :id
        """)
    int increaseFailCountById(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 실패 횟수가 상한에 도달한 PENDING 작업을 FAILED로 전환한다.
     * payload는 원인 확인과 수동 재처리를 위해 유지한다.
     *
     * @return 전환된 행 수
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Outbox o
        SET o.status = com.forgather.global.outbox.OutboxStatus.FAILED, o.updatedAt = :now
        WHERE o.type = :type
          AND o.status = com.forgather.global.outbox.OutboxStatus.PENDING
          AND o.failCount >= :maxFailCount
        """)
    int failExhaustedByType(
        @Param("type") OutboxType type,
        @Param("maxFailCount") int maxFailCount,
        @Param("now") LocalDateTime now
    );

    default Outbox getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("outbox의 id는 null일 수 없습니다.");
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 outbox입니다. id: " + id));
    }
}
