package com.forgather.domain.notification.dto;

import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterNotificationDeviceRequest(

    @Schema(description = "디바이스 플랫폼", example = "IOS")
    @NotNull
    DevicePlatform platform,

    @Schema(description = "FCM 등록 토큰 (최대 512자)", example = "fcm-registration-token", maxLength = 512)
    @NotBlank
    @Size(max = NotificationDevice.MAX_TOKEN_LENGTH)
    String token
) {
}
