package com.forgather.domain.notification.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

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
 * 호스트에게 보내는 알림 한 건.
 * 제목·본문은 생성 시점에 렌더링된 문자열로 저장해 템플릿이 바뀌어도 과거 알림이 변하지 않게 한다.
 */
@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends SoftDeleteEntity {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_BODY_LENGTH = 500;
    /**
     * 검증은 표시 글자 수(복합 이모지 1자)로 하고 DB는 코드 포인트로 세므로, 컬럼은 10배로 잡는다 (V34와 같은 규칙).
     */
    private static final int TITLE_COLUMN_LENGTH = MAX_TITLE_LENGTH * 10;
    private static final int BODY_COLUMN_LENGTH = MAX_BODY_LENGTH * 10;
    private static final int MAX_DEDUPE_KEY_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = TITLE_COLUMN_LENGTH)
    private String title;

    @Column(name = "body", nullable = false, length = BODY_COLUMN_LENGTH)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private NotificationTargetType targetType;

    @Column(name = "target_key", length = NotificationTarget.MAX_KEY_LENGTH)
    private String targetKey;

    /**
     * 스케줄 생성 알림의 중복 실행 방지 키. DB UNIQUE로 같은 키의 두 번째 생성을 막는다.
     * 이벤트 기반 알림은 null이다.
     */
    @Column(name = "dedupe_key", length = MAX_DEDUPE_KEY_LENGTH)
    private String dedupeKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 공급자가 전송 요청을 접수한 시각. 단말 도달 시각이 아니다.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public Notification(
        Host host,
        NotificationType type,
        String title,
        String body,
        NotificationTarget target,
        String dedupeKey
    ) {
        validateRequiredFields(host, type, title, body);
        validateTitle(title);
        validateBody(body);
        validateDedupeKey(dedupeKey);
        this.host = host;
        this.type = type;
        this.title = title;
        this.body = body;
        if (target != null) {
            this.targetType = target.type();
            this.targetKey = target.key();
        }
        this.dedupeKey = dedupeKey;
    }

    /**
     * 종류의 템플릿으로 제목·본문을 렌더링해 생성한다.
     */
    public static Notification of(
        Host host,
        NotificationType type,
        NotificationTarget target,
        String dedupeKey,
        Object... templateArgs
    ) {
        // 렌더링 전에 먼저 검증해, host가 null인 경우 템플릿 오류가 아니라 실제 원인이 보고되게 한다.
        validateHost(host);
        validateType(type);
        return new Notification(
            host,
            type,
            type.renderTitle(templateArgs),
            type.renderBody(templateArgs),
            target,
            dedupeKey
        );
    }

    private static void validateHost(Host host) {
        if (host == null) {
            throw new BaseNullPointerException("알림 수신 호스트는 null일 수 없습니다.");
        }
    }

    private static void validateType(NotificationType type) {
        if (type == null) {
            throw new BaseNullPointerException("알림 종류는 null일 수 없습니다.");
        }
    }

    private void validateRequiredFields(Host host, NotificationType type, String title, String body) {
        validateHost(host);
        validateType(type);
        if (title == null) {
            throw new BaseNullPointerException("알림 제목은 null일 수 없습니다.");
        }
        if (body == null) {
            throw new BaseNullPointerException("알림 본문은 null일 수 없습니다.");
        }
    }

    /**
     * 제목·본문·중복 방지 키는 서버가 템플릿과 스케줄러로 만드는 값이라 검증 실패는 서버 오류로 분류한다.
     * 사용자가 자유 문구를 입력하는 경로가 생기면 그 경로에서 400으로 바꾼다.
     */
    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new BaseException("알림 제목은 공백만 입력할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        int length = TextLengthCounter.count(title);
        if (length > MAX_TITLE_LENGTH) {
            throw new BaseException("알림 제목은 최대 %d자까지 가능합니다. title.length: %d"
                .formatted(MAX_TITLE_LENGTH, length), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateBody(String body) {
        if (body.isBlank()) {
            throw new BaseException("알림 본문은 공백만 입력할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        int length = TextLengthCounter.count(body);
        if (length > MAX_BODY_LENGTH) {
            throw new BaseException("알림 본문은 최대 %d자까지 가능합니다. body.length: %d"
                .formatted(MAX_BODY_LENGTH, length), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateDedupeKey(String dedupeKey) {
        if (dedupeKey == null) {
            return;
        }
        if (dedupeKey.isBlank()) {
            throw new BaseException("알림 중복 방지 키는 공백만 입력할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (dedupeKey.length() > MAX_DEDUPE_KEY_LENGTH) {
            throw new BaseException("알림 중복 방지 키는 최대 %d자까지 가능합니다. dedupeKey.length: %d"
                .formatted(MAX_DEDUPE_KEY_LENGTH, dedupeKey.length()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean hasTarget() {
        return targetType != null;
    }

    public boolean isSent() {
        return sentAt != null;
    }

    public boolean isRead() {
        return readAt != null;
    }

    /**
     * 전송 접수 시각을 기록한다. 이미 기록돼 있으면 처음 값을 유지한다(재시도 시 멱등).
     */
    public void markSent(LocalDateTime sentAt) {
        if (sentAt == null) {
            throw new BaseNullPointerException("전송 접수 시각은 null일 수 없습니다.");
        }
        if (this.sentAt != null) {
            return;
        }
        this.sentAt = sentAt;
    }

    public void read() {
        if (readAt != null) {
            return;
        }
        readAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Notification))
            return false;
        Notification notification = (Notification)o;
        return id != null && Objects.equals(id, notification.id);
    }

    @Override
    public int hashCode() {
        return Notification.class.hashCode();
    }
}
