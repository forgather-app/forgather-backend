package com.forgather.fixture;

import java.util.List;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.model.NotificationTarget;
import com.forgather.domain.notification.model.NotificationType;
import com.forgather.domain.notification.service.NotificationCommand;

public class NotificationFixture {

    public static final String SPACE_CODE = "abcdefghij";
    public static final int TEMPLATE_ARG = 3;

    public static Notification createNotification() {
        return createNotificationWithHost(HostFixture.createHost());
    }

    public static Notification createNotificationWithHost(Host host) {
        return Notification.of(
            host,
            NotificationType.PLACEHOLDER,
            NotificationTarget.space(SPACE_CODE),
            null,
            TEMPLATE_ARG
        );
    }

    public static Notification createNotificationWithDedupeKey(String dedupeKey) {
        return Notification.of(
            HostFixture.createHost(),
            NotificationType.PLACEHOLDER,
            NotificationTarget.space(SPACE_CODE),
            dedupeKey,
            TEMPLATE_ARG
        );
    }

    public static Notification createWithTitleAndBody(String title, String body) {
        return new Notification(HostFixture.createHost(), NotificationType.PLACEHOLDER, title, body, null, null);
    }

    public static NotificationCommand createCommandWithHost(Host host) {
        return createCommandWithHostAndDedupeKey(host, null);
    }

    public static NotificationCommand createCommandWithHostAndDedupeKey(Host host, String dedupeKey) {
        return new NotificationCommand(
            host,
            NotificationType.PLACEHOLDER,
            NotificationTarget.space(SPACE_CODE),
            dedupeKey,
            List.of(TEMPLATE_ARG)
        );
    }
}
