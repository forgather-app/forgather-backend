package com.forgather.domain.notification.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 호스트의 푸시 디바이스 토큰.
 * 토큰은 호스트가 아니라 기기에 속한다. 같은 기기에 다른 호스트가 로그인하면 소유자가 바뀐다.
 */
@Getter
@Entity
@Table(name = "notification_device")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDevice extends SoftDeleteEntity {

    public static final int MAX_TOKEN_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    /**
     * 푸시 공급자(FCM)가 발급한 등록 토큰. soft delete된 행까지 포함해 DB UNIQUE다.
     */
    @Column(name = "token", nullable = false, length = MAX_TOKEN_LENGTH)
    private String token;

    /**
     * 토큰 등록·갱신 시각. 장기 미사용 토큰 정리 기준으로 쓴다.
     */
    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    public NotificationDevice(Host host, DevicePlatform platform, String token) {
        validateRequiredFields(host, platform, token);
        validateToken(token);
        this.host = host;
        this.platform = platform;
        this.token = token;
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 같은 토큰이 다시 등록될 때 호출한다.
     * 소유 호스트와 플랫폼을 새 값으로 바꾸고 활동 시각을 갱신하며, 논리 삭제된 행이면 복구한다.
     * 같은 호스트의 재등록(앱 재실행, 토큰 갱신 확인)도 이 메서드를 탄다.
     */
    public void reassign(Host host, DevicePlatform platform) {
        validateOwner(host, platform);
        this.host = host;
        this.platform = platform;
        this.lastActiveAt = LocalDateTime.now();
        restore();
    }

    private void validateRequiredFields(Host host, DevicePlatform platform, String token) {
        validateOwner(host, platform);
        if (token == null) {
            throw new BaseNullPointerException("디바이스 토큰은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 생성과 재등록이 공유하는 소유자 검증. 플랫폼은 요청 본문에서 오므로 400으로 분류한다.
     */
    private void validateOwner(Host host, DevicePlatform platform) {
        if (host == null) {
            throw new BaseNullPointerException("디바이스 호스트는 null일 수 없습니다.");
        }
        if (platform == null) {
            throw new BaseNullPointerException("디바이스 플랫폼은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * FCM 토큰은 공백 없는 ASCII 문자열이다. 길이 상한은 공개되지 않아 컬럼 길이로 제한한다.
     */
    private void validateToken(String token) {
        if (token.isBlank()) {
            throw new BaseException("디바이스 토큰은 공백만 입력할 수 없습니다.");
        }
        if (token.chars().anyMatch(Character::isWhitespace)) {
            throw new BaseException("디바이스 토큰에는 공백을 포함할 수 없습니다.");
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new BaseException("디바이스 토큰은 최대 %d자까지 가능합니다. token.length: %d"
                .formatted(MAX_TOKEN_LENGTH, token.length()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof NotificationDevice))
            return false;
        NotificationDevice device = (NotificationDevice)o;
        return id != null && Objects.equals(id, device.id);
    }

    @Override
    public int hashCode() {
        return NotificationDevice.class.hashCode();
    }
}
