package com.forgather.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

class FailureTypeTest {

    @DisplayName("우리 쪽 결함은 500과 error로, 외부 장애는 503과 warn으로 매핑된다")
    @Test
    void statusAndLevel() {
        // when & then
        assertAll(
            () -> assertThat(FailureType.CALLER_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
            () -> assertThat(FailureType.CALLER_ERROR.logLevel()).isEqualTo(Level.ERROR),
            () -> assertThat(FailureType.MALFORMED_RESPONSE.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
            () -> assertThat(FailureType.MALFORMED_RESPONSE.logLevel()).isEqualTo(Level.ERROR),
            () -> assertThat(FailureType.UPSTREAM_ERROR.httpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE),
            () -> assertThat(FailureType.UPSTREAM_ERROR.logLevel()).isEqualTo(Level.WARN)
        );
    }

    @DisplayName("사용자 입력 문제는 401과 warn으로 매핑된다")
    @Test
    void authRejected() {
        // when & then
        assertAll(
            () -> assertThat(FailureType.AUTH_REJECTED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED),
            () -> assertThat(FailureType.AUTH_REJECTED.logLevel()).isEqualTo(Level.WARN)
        );
    }

    @DisplayName("connect timeout과 연결 실패는 멱등 여부와 무관하게 재시도 가능하다")
    @Test
    void alwaysRetryable() {
        // when & then
        assertAll(
            () -> assertThat(FailureType.CONNECT_TIMEOUT.retryable(false)).isTrue(),
            () -> assertThat(FailureType.CONNECTION_FAILED.retryable(false)).isTrue(),
            () -> assertThat(FailureType.RATE_LIMITED.retryable(false)).isTrue(),
            () -> assertThat(FailureType.UPSTREAM_ERROR.retryable(false)).isTrue()
        );
    }

    @DisplayName("read timeout은 요청 처리 여부를 알 수 없으므로 멱등한 연산만 재시도한다")
    @Test
    void readTimeoutDependsOnIdempotency() {
        // when & then
        assertAll(
            () -> assertThat(FailureType.READ_TIMEOUT.retryable(true)).isTrue(),
            () -> assertThat(FailureType.READ_TIMEOUT.retryable(false)).isFalse()
        );
    }

    @DisplayName("우리 쪽 결함은 멱등해도 재시도하지 않는다")
    @Test
    void neverRetryable() {
        // when & then
        assertAll(
            () -> assertThat(FailureType.CALLER_ERROR.retryable(true)).isFalse(),
            () -> assertThat(FailureType.MALFORMED_RESPONSE.retryable(true)).isFalse(),
            () -> assertThat(FailureType.AUTH_REJECTED.retryable(true)).isFalse()
        );
    }

    @DisplayName("authorization code는 1회용이므로 APPLE_TOKEN은 비멱등이다")
    @Test
    void operationIdempotency() {
        // when & then
        assertAll(
            () -> assertThat(ExternalOperation.APPLE_TOKEN.idempotent()).isFalse(),
            () -> assertThat(ExternalOperation.APPLE_REVOKE.idempotent()).isTrue(),
            () -> assertThat(ExternalOperation.KAKAO_UNLINK.idempotent()).isTrue(),
            () -> assertThat(ExternalOperation.jwks(ExternalService.APPLE).idempotent()).isTrue()
        );
    }

    @DisplayName("연산 식별자는 로그와 메시지에 쓰기 좋은 형태로 만들어진다")
    @Test
    void operationId() {
        // when & then
        assertAll(
            () -> assertThat(ExternalOperation.APPLE_TOKEN.id()).isEqualTo("apple/token"),
            () -> assertThat(ExternalOperation.jwks(ExternalService.KAKAO).id()).isEqualTo("kakao/jwks")
        );
    }
}
