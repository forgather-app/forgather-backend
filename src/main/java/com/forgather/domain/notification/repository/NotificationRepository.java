package com.forgather.domain.notification.repository;

import com.forgather.domain.notification.model.Notification;

public interface NotificationRepository {

    Notification save(Notification notification);

    /**
     * dedupeKey UNIQUE 충돌을 서비스 트랜잭션 안에서 잡기 위해 즉시 flush한다.
     */
    Notification saveAndFlush(Notification notification);

    boolean existsByDedupeKey(String dedupeKey);
}
