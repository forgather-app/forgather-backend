package com.forgather.domain.notification.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.Notification;

public interface NotificationRepository {

    Notification save(Notification notification);

    /**
     * dedupeKey UNIQUE 충돌을 서비스 트랜잭션 안에서 잡기 위해 즉시 flush한다.
     */
    Notification saveAndFlush(Notification notification);

    boolean existsByDedupeKey(String dedupeKey);

    /**
     * 호스트의 알림함 목록. 삭제된 알림은 제외한다. 정렬은 서비스가 createdAt·id 내림차순으로 고정해 넘긴다.
     * host_id 등치 조건과 created_at 정렬이라 IX_notification_host_id_created_at을 탄다.
     */
    Page<Notification> findAllByHostAndDeletedAtIsNull(Host host, Pageable pageable);

    long countByHostAndReadAtIsNullAndDeletedAtIsNull(Host host);

    /**
     * 호스트의 읽지 않은 알림을 모두 읽음 처리한다. 멱등이며 이미 읽은 행과 삭제된 행은 건드리지 않는다.
     * 호출 시점의 미읽음 전체가 대상이다.
     * 영속성 컨텍스트에 반영되지 않으므로, 같은 트랜잭션에서 대상 알림을 로드한 뒤 호출하거나 호출 뒤 다시 읽지 않는다.
     *
     * @return 갱신된 행 수
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Notification n
        SET n.readAt = :now, n.updatedAt = :now
        WHERE n.host = :host
            AND n.readAt IS NULL
            AND n.deletedAt IS NULL
        """)
    int readAllByHost(@Param("host") Host host, @Param("now") LocalDateTime now);
}
