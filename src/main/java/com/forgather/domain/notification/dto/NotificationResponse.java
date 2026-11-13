package com.forgather.domain.notification.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.model.NotificationTargetType;
import com.forgather.domain.notification.model.NotificationType;

/**
 * 알림함 목록 항목. targetType·targetKey·readAt은 null일 수 있다.
 * 지연 로딩을 피하기 위해 host는 읽지 않는다.
 */
public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String body,
    NotificationTargetType targetType,
    String targetKey,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getBody(),
            notification.getTargetType(),
            notification.getTargetKey(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
