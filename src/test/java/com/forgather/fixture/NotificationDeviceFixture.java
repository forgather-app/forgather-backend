package com.forgather.fixture;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;

public class NotificationDeviceFixture {

    public static final String TOKEN = "fcm-registration-token-0001";

    public static NotificationDevice createDevice() {
        return createDeviceWithHost(HostFixture.createHost());
    }

    public static NotificationDevice createDeviceWithHost(Host host) {
        return new NotificationDevice(host, DevicePlatform.IOS, TOKEN);
    }

    public static NotificationDevice createDeviceWithToken(String token) {
        return new NotificationDevice(HostFixture.createHost(), DevicePlatform.IOS, token);
    }
}
