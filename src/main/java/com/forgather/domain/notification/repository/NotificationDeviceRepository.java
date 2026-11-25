package com.forgather.domain.notification.repository;

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
     * 토큰 UNIQUE 위반을 등록 시도 안에서 잡을 수 있도록 즉시 flush한다.
     */
    NotificationDevice saveAndFlush(NotificationDevice notificationDevice);

    /**
     * 재등록 시 소유자를 갱신할 디바이스를 토큰으로 조회한다.
     */
    Optional<NotificationDevice> findByToken(String token);

    List<NotificationDevice> findAllByHost(Host host);

    /**
     * 토큰·소유 호스트가 일치하는 행만 삭제한다. 소유권이 먼저 변경됐다면 삭제하지 않는다.
     *
     * @return 삭제된 행 수 (0 또는 1)
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM NotificationDevice d
        WHERE d.token = :token
            AND d.host = :host
        """)
    int deleteByTokenAndHost(@Param("token") String token, @Param("host") Host host);

    /**
     * 탈퇴 호스트의 디바이스를 벌크 삭제한다.
     * 영속성 컨텍스트에 반영되지 않으므로, 같은 트랜잭션에서 대상 엔티티를 로드하기 전에 호출한다.
     *
     * @return 삭제된 행 수
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM NotificationDevice d
        WHERE d.host = :host
        """)
    int deleteAllByHost(@Param("host") Host host);
}
