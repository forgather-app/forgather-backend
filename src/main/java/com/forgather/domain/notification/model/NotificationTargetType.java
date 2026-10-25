package com.forgather.domain.notification.model;

/**
 * 알림을 탭했을 때 이동할 대상의 종류.
 * 새 종류 추가 시 DDL 변경은 필요 없다 (notification.target_type은 VARCHAR).
 */
public enum NotificationTargetType {
    SPACE,
    ;
}
