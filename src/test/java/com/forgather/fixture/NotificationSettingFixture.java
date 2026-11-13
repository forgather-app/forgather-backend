package com.forgather.fixture;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.NotificationSetting;

public class NotificationSettingFixture {

    public static NotificationSetting createEnabled(Host host) {
        return new NotificationSetting(host, true);
    }

    public static NotificationSetting createDisabled(Host host) {
        return new NotificationSetting(host, false);
    }
}
