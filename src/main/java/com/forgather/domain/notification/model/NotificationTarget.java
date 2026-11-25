package com.forgather.domain.notification.model;

import org.springframework.http.HttpStatus;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

/**
 * 알림을 탭했을 때 이동할 대상. 전송 시 딥링크 data로 전달한다.
 * 키는 서버가 만드는 값(스페이스 코드 등)이라 검증 실패는 서버 오류로 분류한다.
 */
public record NotificationTarget(
    NotificationTargetType type,
    String key
) {

    public static final int MAX_KEY_LENGTH = 255;

    public NotificationTarget {
        if (type == null) {
            throw new BaseNullPointerException("알림 대상 종류는 null일 수 없습니다.");
        }
        if (key == null) {
            throw new BaseNullPointerException("알림 대상 키는 null일 수 없습니다.");
        }
        if (key.isBlank()) {
            throw new BaseException("알림 대상 키는 공백만 입력할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new BaseException("알림 대상 키는 최대 %d자까지 가능합니다. key.length: %d"
                .formatted(MAX_KEY_LENGTH, key.length()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static NotificationTarget space(String spaceCode) {
        return new NotificationTarget(NotificationTargetType.SPACE, spaceCode);
    }
}
