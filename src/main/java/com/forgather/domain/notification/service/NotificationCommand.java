package com.forgather.domain.notification.service;

import java.util.List;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.NotificationTarget;
import com.forgather.domain.notification.model.NotificationType;
import com.forgather.global.exception.BaseNullPointerException;

/**
 * 알림 생성 요청. 기능 도메인이 {@link NotificationService#create(NotificationCommand)}에 넘긴다.
 * {@code Notification.of}의 varargs 자리에 dedupeKey를 잘못 넣는 실수를 막기 위해 필드를 이름으로 구분한다.
 *
 * @param recipient    수신 호스트
 * @param type         알림 종류 (제목·본문 템플릿을 가진다)
 * @param target       탭 시 이동할 대상. 없으면 null
 * @param dedupeKey    스케줄 생성 알림의 중복 실행 방지 키. 이벤트 기반 알림은 null
 * @param templateArgs 템플릿 인자. 없으면 빈 리스트
 */
public record NotificationCommand(
    Host recipient,
    NotificationType type,
    NotificationTarget target,
    String dedupeKey,
    List<Object> templateArgs
) {

    public NotificationCommand {
        if (recipient == null) {
            throw new BaseNullPointerException("알림 수신 호스트는 null일 수 없습니다.");
        }
        if (type == null) {
            throw new BaseNullPointerException("알림 종류는 null일 수 없습니다.");
        }
        templateArgs = templateArgs == null ? List.of() : List.copyOf(templateArgs);
    }
}
