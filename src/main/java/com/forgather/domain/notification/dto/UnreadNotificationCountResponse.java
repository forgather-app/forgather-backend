package com.forgather.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadNotificationCountResponse(

    @Schema(description = "읽지 않은 알림 개수. 99+ 같은 표시 상한은 클라이언트가 처리한다", example = "3")
    long unreadCount
) {
}
