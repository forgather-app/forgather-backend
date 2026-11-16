package com.forgather.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationSettingResponse(

    @Schema(description = "알림 수신 여부. 설정을 변경한 적이 없으면 true", example = "true")
    boolean enabled
) {
}
