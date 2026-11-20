package com.forgather.domain.notification.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.notification.model.NotificationSetting;
import com.forgather.domain.notification.repository.NotificationSettingRepository;

public interface NotificationSettingJpaRepository
    extends JpaRepository<NotificationSetting, Long>, NotificationSettingRepository {
}
