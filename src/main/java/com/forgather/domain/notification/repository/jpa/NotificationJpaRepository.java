package com.forgather.domain.notification.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.repository.NotificationRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long>, NotificationRepository {
}
