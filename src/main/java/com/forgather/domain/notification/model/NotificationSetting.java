package com.forgather.domain.notification.model;

import java.util.Objects;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 호스트별 푸시 알림 수신 설정. 호스트와 1:1이며, 행이 없으면 허용으로 간주한다.
 * 알림함(인앱) 알림은 이 설정과 무관하게 항상 만들어지고, 이 설정은 푸시를 보낼지만 결정한다.
 * 삭제 경로가 없는 설정 행이라 soft delete하지 않고, 탈퇴 시에도 행을 남긴다.
 * <p>
 * 상태 변경 메서드를 두지 않는다. 기존 행의 갱신은 리포지토리의 명시적 UPDATE로만 한다.
 * 변경 감지는 로드 시점과 같은 값을 대입하면 UPDATE를 내지 않아, 동시 요청의 변경을 덮어쓰지 못하기 때문이다.
 */
@Getter
@Entity
@Table(
    name = "notification_setting",
    uniqueConstraints = @UniqueConstraint(name = NotificationSetting.UK_HOST_ID, columnNames = "host_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    /**
     * host_id UNIQUE 제약 이름 (V45). 동시 최초 저장의 UNIQUE 위반을 판별할 때 쓴다.
     */
    public static final String UK_HOST_ID = "UK_notification_setting_host_id";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    /**
     * 푸시 알림 수신 여부. 꺼져 있어도 알림함 알림은 만들어지고, 푸시만 보내지 않는다.
     */
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    public NotificationSetting(Host host, boolean pushEnabled) {
        validateHost(host);
        this.host = host;
        this.pushEnabled = pushEnabled;
    }

    private void validateHost(Host host) {
        if (host == null) {
            throw new BaseNullPointerException("알림 설정의 호스트는 null일 수 없습니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof NotificationSetting))
            return false;
        NotificationSetting setting = (NotificationSetting)o;
        return id != null && Objects.equals(id, setting.id);
    }

    @Override
    public int hashCode() {
        return NotificationSetting.class.hashCode();
    }
}
