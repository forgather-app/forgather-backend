package com.forgather.domain.notification.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;

public interface NotificationDeviceJpaRepository
    extends JpaRepository<NotificationDevice, Long>, NotificationDeviceRepository {
}
