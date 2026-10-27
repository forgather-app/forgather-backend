package com.forgather.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.NotificationDevice;

public interface NotificationDeviceRepository {

    NotificationDevice save(NotificationDevice notificationDevice);

    /**
     * token UNIQUE 충돌(동시 최초 등록)을 서비스 트랜잭션 안에서 잡기 위해 즉시 flush한다.
     */
    NotificationDevice saveAndFlush(NotificationDevice notificationDevice);

    /**
     * soft delete된 행까지 포함해 조회한다. 같은 토큰이 다시 등록될 때 기존 행을 복구하는 용도다.
     */
    Optional<NotificationDevice> findByToken(String token);

    Optional<NotificationDevice> findByTokenAndHostAndDeletedAtIsNull(String token, Host host);

    List<NotificationDevice> findAllByHostAndDeletedAtIsNull(Host host);

    /**
     * 호스트의 활성 디바이스를 벌크 UPDATE로 soft delete한다 (탈퇴용).
     * 엔티티를 로드하지 않으므로 {@code SoftDeleteEntity.delete()}와 auditing을 거치지 않는다.
     * updatedAt은 직접 세팅하며, {@code NotificationDevice.delete()}에 부가 로직이 생기면 이 경로도 함께 수정해야 한다.
     * 같은 트랜잭션에서 먼저 로드한 디바이스 엔티티에는 결과가 반영되지 않으므로 clearAutomatically는 쓰지 않고,
     * 대상 엔티티를 로드하기 전에 호출해야 한다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE NotificationDevice d
        SET d.deletedAt = :now, d.updatedAt = :now
        WHERE d.host = :host
            AND d.deletedAt IS NULL
        """)
    int softDeleteAllByHost(@Param("host") Host host, @Param("now") LocalDateTime now);
}
