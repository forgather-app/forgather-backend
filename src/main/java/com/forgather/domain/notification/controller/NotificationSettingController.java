package com.forgather.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.dto.NotificationSettingResponse;
import com.forgather.domain.notification.dto.UpdateNotificationSettingRequest;
import com.forgather.domain.notification.service.NotificationSettingService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification: 알림", description = "알림 관련 API")
@RequiredArgsConstructor
@RequestMapping("/notifications/settings")
@RestController
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "알림 설정 조회",
        description = "로그인한 호스트의 알림 설정을 조회합니다. 설정을 변경한 적이 없으면 기본값 true를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getSetting(@LoginHost Host host) {
        NotificationSettingResponse response = notificationSettingService.getSetting(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "알림 설정 변경",
        description = "로그인한 호스트의 알림 수신 여부를 변경합니다. 끄면 그 뒤로 알림을 만들지 않으며, "
            + "이미 생성된 알림은 알림함에 남습니다. 최초 저장 경합은 서버가 한 번 재시도하며, "
            + "재시도에서도 충돌하면 409 Conflict를 반환합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateSetting(
        @LoginHost Host host,
        @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        NotificationSettingResponse response = notificationSettingService.update(host, request.enabled());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
