package com.forgather.domain.notification.dto;

import java.time.LocalDateTime;

import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.model.NotificationTargetType;
import com.forgather.domain.notification.model.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림함 목록 항목. targetType·targetKey·readAt은 null일 수 있다.
 * 지연 로딩을 피하기 위해 host는 읽지 않는다.
 */
public record NotificationResponse(

    @Schema(description = "알림 ID", example = "1")
    Long id,

    @Schema(description = "알림 종류. 클라이언트는 이 값으로 아이콘 등 표시 방식을 고른다")
    NotificationType type,

    @Schema(description = "알림 제목. 생성 시점에 렌더링해 저장한 문자열이며 최대 100자", example = "레오님이 방명록을 남겼어요")
    String title,

    @Schema(description = "알림 본문. 생성 시점에 렌더링해 저장한 문자열이며 최대 500자", example = "전시 잘봤다~~ 너가 최고야 🤙")
    String body,

    @Schema(
        description = "알림을 눌렀을 때 이동할 대상의 종류. SPACE: 스페이스. 이동할 대상이 없으면 null",
        example = "SPACE",
        nullable = true)
    NotificationTargetType targetType,

    @Schema(
        description = "이동할 대상의 키. targetType이 SPACE이면 스페이스 코드. 이동할 대상이 없으면 null",
        example = "1234567890",
        nullable = true)
    String targetKey,

    @Schema(description = "읽음 처리 시각. 읽지 않았으면 null", example = "2026-11-01T16:05:00", nullable = true)
    LocalDateTime readAt,

    @Schema(description = "알림 생성 시각", example = "2026-11-01T16:00:00")
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
