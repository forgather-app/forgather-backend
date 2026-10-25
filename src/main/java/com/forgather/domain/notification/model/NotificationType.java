package com.forgather.domain.notification.model;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.forgather.global.exception.BaseException;

/**
 * 알림 종류와 문구 템플릿.
 * 새 종류 추가 시 DDL 변경은 필요 없다 (notification.type은 VARCHAR(50)이므로 상수 이름은 50자 이내).
 * 템플릿은 {@link String#formatted(Object...)} 형식이고, 생성 시점에 렌더링한 문자열을 저장한다.
 * 제목과 본문이 같은 인자 배열을 나눠 쓰므로 남는 인자는 허용한다.
 */
public enum NotificationType {

    /**
     * 공통 알림 도메인 구축 단계에서 엔티티·서비스 테스트를 위해 둔 임시 상수다. 실제 알림 종류가 아니다.
     * enum은 상수가 하나도 없으면 {@link Notification}을 만들 수 없어 공통 코드를 검증할 수 없기 때문에 둔다.
     */
    // TODO #224 첫 알림 기능(읽지 않은 방명록 다이제스트) 구현 시 이 상수를 삭제하고 실제 종류로 교체한다.
    PLACEHOLDER("임시 알림", "임시 알림 본문 %d"),
    ;

    private final String titleTemplate;
    private final String bodyTemplate;

    NotificationType(String titleTemplate, String bodyTemplate) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public String renderTitle(Object... args) {
        return render(titleTemplate, args);
    }

    public String renderBody(Object... args) {
        return render(bodyTemplate, args);
    }

    /**
     * 템플릿 인자 불일치는 호출 코드의 버그이므로 서버 오류로 분류한다.
     * null 인자는 String.formatted가 "null" 문자열로 조용히 렌더링하므로 먼저 걸러낸다.
     */
    private String render(String template, Object... args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new BaseException(
                "알림 템플릿 인자는 null일 수 없습니다. type: " + name(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        try {
            return template.formatted(args);
        } catch (IllegalFormatException e) {
            throw new BaseException(
                "알림 템플릿 인자가 올바르지 않습니다. type: %s, args: %s".formatted(name(), Arrays.toString(args)),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e
            );
        }
    }
}
