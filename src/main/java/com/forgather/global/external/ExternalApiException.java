package com.forgather.global.external;

import java.net.SocketTimeoutException;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.forgather.global.exception.BaseException;

import lombok.Getter;

/**
 * 외부 서비스 호출 실패를 나타내는 단일 예외 타입.
 * nginx가 내는 502(앱 다운)·504(앱 응답 지연)와 겹치지 않도록 외부 장애에는 503을 쓴다.
 */
@Getter
public class ExternalApiException extends BaseException {

    private static final int TOO_MANY_REQUESTS = 429;
    private static final String CONNECT_TIMEOUT_MARKER = "Connect";

    private final ExternalOperation operation;
    private final FailureType type;
    private final String responseBody;

    public ExternalApiException(
        ExternalOperation operation,
        FailureType type,
        String responseBody,
        String message,
        Throwable cause
    ) {
        super(message, type.httpStatus(), cause);
        this.operation = operation;
        this.type = type;
        this.responseBody = responseBody;
    }

    public ExternalApiException(ExternalOperation operation, FailureType type, String message) {
        this(operation, type, null, message, null);
    }

    /**
     * 상태코드만 보고 분류한다. provider별 본문 규약은 각 클라이언트가 2차로 세분화한다.
     */
    public static ExternalApiException fromStatus(ExternalOperation operation, RestClientResponseException cause) {
        HttpStatusCode status = cause.getStatusCode();
        FailureType type = resolveStatusType(status);
        String message = "%s 호출이 %d로 실패했습니다.".formatted(operation.id(), status.value());
        return new ExternalApiException(operation, type, cause.getResponseBodyAsString(), message, cause);
    }

    public static ExternalApiException fromIo(ExternalOperation operation, ResourceAccessException cause) {
        FailureType type = resolveIoType(cause);
        String message = "%s 호출에 실패했습니다. 원인: %s".formatted(operation.id(), type);
        return new ExternalApiException(operation, type, null, message, cause);
    }

    /**
     * 2차 세분화용. 응답 본문을 읽어야만 아는 사실을 반영한 새 예외를 만든다.
     */
    public ExternalApiException as(FailureType refined) {
        return new ExternalApiException(operation, refined, responseBody, getMessage(), getCause());
    }

    public boolean isRetryable() {
        return type.retryable(operation.idempotent());
    }

    public Level getLogLevel() {
        return type.logLevel();
    }

    private static FailureType resolveStatusType(HttpStatusCode status) {
        if (status.value() == TOO_MANY_REQUESTS) {
            return FailureType.RATE_LIMITED;
        }
        if (status.is5xxServerError()) {
            return FailureType.UPSTREAM_ERROR;
        }
        return FailureType.CALLER_ERROR;
    }

    private static FailureType resolveIoType(ResourceAccessException cause) {
        if (cause.getCause() instanceof SocketTimeoutException timeout) {
            String message = String.valueOf(timeout.getMessage());
            return message.contains(CONNECT_TIMEOUT_MARKER) ? FailureType.CONNECT_TIMEOUT : FailureType.READ_TIMEOUT;
        }
        return FailureType.CONNECTION_FAILED;
    }
}
