package com.forgather.global.external;

import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

/**
 * 외부 호출 실패의 원인 분류이자 정책 테이블.
 * 응답 status, 로그 레벨, 재시도 가능 여부가 모두 여기서 파생된다.
 */
public enum FailureType {

    /** 우리가 요청을 잘못 만들었다. 설정 오류 등 배포로만 고쳐진다. */
    CALLER_ERROR(INTERNAL_SERVER_ERROR, ERROR, Retry.NEVER),

    /** 200이지만 응답 형식이 계약과 다르다. 우리 DTO가 어긋났을 가능성이 높다. */
    MALFORMED_RESPONSE(INTERNAL_SERVER_ERROR, ERROR, Retry.NEVER),

    /** 사용자 입력 문제. 코드 재사용·만료 등으로 재로그인이 필요하다. */
    AUTH_REJECTED(UNAUTHORIZED, WARN, Retry.NEVER),

    /** 레이트리밋. 우리 결함도 사용자 결함도 아니고 백오프 대상이다. */
    RATE_LIMITED(SERVICE_UNAVAILABLE, WARN, Retry.ALWAYS),

    /** 외부 5xx. */
    UPSTREAM_ERROR(SERVICE_UNAVAILABLE, WARN, Retry.ALWAYS),

    /** 요청이 상대에게 도달하지 못했다. 부작용이 없음이 보장되므로 항상 재시도 가능하다. */
    CONNECT_TIMEOUT(SERVICE_UNAVAILABLE, WARN, Retry.ALWAYS),

    /** 요청은 갔고 응답만 못 받았다. 상대가 처리했는지 알 수 없다. */
    READ_TIMEOUT(SERVICE_UNAVAILABLE, WARN, Retry.IF_IDEMPOTENT),

    /** DNS 실패, connection refused 등. */
    CONNECTION_FAILED(SERVICE_UNAVAILABLE, WARN, Retry.ALWAYS);

    private enum Retry {
        NEVER,
        ALWAYS,
        IF_IDEMPOTENT
    }

    private final HttpStatus httpStatus;
    private final Level logLevel;
    private final Retry retry;

    FailureType(HttpStatus httpStatus, Level logLevel, Retry retry) {
        this.httpStatus = httpStatus;
        this.logLevel = logLevel;
        this.retry = retry;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public Level logLevel() {
        return logLevel;
    }

    public boolean retryable(boolean idempotent) {
        return switch (retry) {
            case NEVER -> false;
            case ALWAYS -> true;
            case IF_IDEMPOTENT -> idempotent;
        };
    }
}
