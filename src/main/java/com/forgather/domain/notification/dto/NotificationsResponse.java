package com.forgather.domain.notification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.forgather.domain.notification.model.Notification;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationsResponse(

    @ArraySchema(
        schema = @Schema(implementation = NotificationResponse.class),
        arraySchema = @Schema(description = "알림 목록. 생성 시각과 ID 내림차순")
    )
    List<NotificationResponse> notifications,

    @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
    int currentPage,

    @Schema(description = "페이지 당 알림 개수", example = "15")
    int pageSize,

    @Schema(description = "총 알림 개수", example = "3")
    long totalCount,

    @Schema(description = "총 페이지 수", example = "1")
    int totalPages
) {

    public static NotificationsResponse from(Page<Notification> notifications) {
        return new NotificationsResponse(
            notifications.getContent().stream().map(NotificationResponse::from).toList(),
            notifications.getNumber() + 1,
            notifications.getSize(),
            notifications.getTotalElements(),
            notifications.getTotalPages()
        );
    }
}
