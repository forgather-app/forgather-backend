package com.forgather.domain.notification.event;

import com.forgather.domain.notification.service.NotificationCommand;
import com.forgather.global.exception.BaseNullPointerException;

/**
 * 알림 생성 요청. 기능 도메인이 자기 트랜잭션 안에서 {@code ApplicationEventPublisher}로 발행한다.
 * 기능 트랜잭션이 커밋되면 {@link NotificationRequestedEventListener}가 별도 트랜잭션에서 알림을 만들고,
 * 롤백되면 알림을 만들지 않는다.
 *
 * @param command 알림 생성 요청 내용
 */
public record NotificationRequestedEvent(
    NotificationCommand command
) {

    public NotificationRequestedEvent {
        if (command == null) {
            throw new BaseNullPointerException("알림 생성 요청은 null일 수 없습니다.");
        }
    }
}
