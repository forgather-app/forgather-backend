package com.forgather.domain.notification.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.dto.NotificationsResponse;
import com.forgather.domain.notification.dto.UnreadNotificationCountResponse;
import com.forgather.domain.notification.service.NotificationService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification: 알림", description = "알림 관련 API")
@RequiredArgsConstructor
@RequestMapping("/notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "알림함 목록 조회",
        description = "로그인한 호스트의 알림을 조회합니다. 페이지는 1부터 시작하고 기본 크기는 15입니다. "
            + "정렬은 요청과 관계없이 생성일과 id 내림차순입니다. 삭제된 알림은 포함하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationsResponse>> getNotifications(
        @LoginHost Host host,
        @PageableDefault(size = 15) Pageable pageable
    ) {
        NotificationsResponse response = notificationService.getNotifications(host, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "읽지 않은 알림 개수 조회",
        description = "로그인한 호스트의 읽지 않은 알림 개수를 조회합니다. 99+ 같은 표시 상한은 클라이언트가 처리합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> countUnread(@LoginHost Host host) {
        UnreadNotificationCountResponse response = notificationService.countUnread(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "알림 전체 읽음 처리",
        description = "로그인한 호스트의 읽지 않은 알림을 모두 읽음 처리합니다. 멱등입니다. "
            + "호출 시점의 미읽음 전체가 대상이므로, 목록을 조회한 뒤 호출 전에 들어온 알림도 읽음 처리됩니다. "
            + "알림 화면에서 1페이지를 조회하고 렌더링한 뒤 호출합니다.")
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Void>> readAll(@LoginHost Host host) {
        notificationService.readAll(host);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
