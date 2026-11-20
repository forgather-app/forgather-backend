package com.forgather.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationSettingResponse(

    @Schema(description = "푸시 알림 수신 여부. 설정을 변경한 적이 없으면 true. 알림함 알림은 이 값과 무관하게 쌓인다",
        example = "true")
    boolean pushEnabled
) {
}
