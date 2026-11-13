package com.forgather.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(

    @Schema(description = "알림 수신 여부", example = "false")
    @NotNull
    Boolean enabled
) {
}
