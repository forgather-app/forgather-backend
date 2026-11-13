package com.forgather.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.NotificationSetting;

public interface NotificationSettingRepository {

    NotificationSetting save(NotificationSetting notificationSetting);

    /**
     * host_id UNIQUE 위반을 저장 시도 안에서 잡을 수 있도록 즉시 flush한다.
     */
    NotificationSetting saveAndFlush(NotificationSetting notificationSetting);

    Optional<NotificationSetting> findByHost(Host host);

    /**
     * 기존 행의 설정을 명시적으로 갱신한다. 값이 같아도 항상 실행해 행 락으로 직렬화하고 마지막에 커밋한 값을 남긴다.
     * 변경 감지는 로드 시점과 같은 값이면 UPDATE를 내지 않아 동시 요청의 변경을 덮어쓰지 못하므로 쓰지 않는다.
     * <p>
     * 호출 전에 행 존재를 비잠금 조회로 확인한다. 행이 없는 상태에서 이 UPDATE를 먼저 실행하면
     * REPEATABLE READ에서 유니크 인덱스 갭 락이 걸려 동시 최초 저장의 INSERT가 데드락에 빠진다.
     * 영속성 컨텍스트에 반영되지 않으므로, 같은 트랜잭션에서 대상 엔티티를 다시 읽지 않는다.
     *
     * @return 매치된 행 수. Connector/J 기본값(useAffectedRows=false)에서는 값이 같아도 1이므로 분기 조건에 쓰지 않는다
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE NotificationSetting s
        SET s.enabled = :enabled, s.updatedAt = :now
        WHERE s.host = :host
        """)
    int updateEnabledByHost(
        @Param("host") Host host,
        @Param("enabled") boolean enabled,
        @Param("now") LocalDateTime now
    );
}
