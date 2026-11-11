package com.forgather.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.dto.RegisterNotificationDeviceRequest;
import com.forgather.domain.notification.service.NotificationDeviceService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification: 알림", description = "알림 관련 API")
@RequiredArgsConstructor
@RequestMapping("/notifications/devices")
@RestController
public class NotificationDeviceController {

    private final NotificationDeviceService notificationDeviceService;

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "푸시 디바이스 토큰 등록",
        description = "푸시 토큰을 등록하며, 같은 토큰은 요청한 호스트·플랫폼·활동 시각으로 갱신합니다. "
            + "로그아웃 전에 DELETE /notifications/devices/{token}으로 토큰을 해제해야 합니다. "
            + "등록 경합은 서버가 한 번 재시도하며, 재시도에서도 토큰 중복이 발생하면 409 Conflict를 반환합니다.")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> register(
        @LoginHost Host host,
        @Valid @RequestBody RegisterNotificationDeviceRequest request
    ) {
        notificationDeviceService.register(host, request.platform(), request.token());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "푸시 디바이스 토큰 해제",
        description = "로그인한 호스트 소유의 푸시 토큰을 해제합니다. 멱등이며 "
            + "토큰이 없거나 다른 호스트 소유여도 200 OK로 응답합니다. 로그아웃 직전에 호출합니다.")
    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponse<Void>> deregister(
        @LoginHost Host host,
        @PathVariable(name = "token") String token
    ) {
        notificationDeviceService.deregister(host, token);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
