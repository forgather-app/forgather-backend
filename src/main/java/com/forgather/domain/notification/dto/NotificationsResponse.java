package com.forgather.domain.notification.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.forgather.domain.notification.model.Notification;

public record NotificationsResponse(
    List<NotificationResponse> notifications,
    int currentPage,
    int pageSize,
    long totalCount,
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
