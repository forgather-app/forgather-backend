package com.forgather.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class ExternalApiExceptionTest {

    @DisplayName("외부 5xx는 UPSTREAM_ERROR로 분류되고 503으로 응답한다")
    @Test
    void fromServerError() {
        // when
        ExternalApiException exception = ExternalApiException.fromStatus(
            ExternalOperation.APPLE_TOKEN, responseException(HttpStatus.BAD_GATEWAY, ""));

        // then
        assertAll(
            () -> assertThat(exception.getType()).isEqualTo(FailureType.UPSTREAM_ERROR),
            () -> assertThat(exception.getStatusCode()).isEqualTo(503),
            () -> assertThat(exception.isRetryable()).isTrue()
        );
    }

    @DisplayName("외부 4xx는 보수적으로 CALLER_ERROR로 분류된다")
    @Test
    void fromClientError() {
        // when
        ExternalApiException exception = ExternalApiException.fromStatus(
            ExternalOperation.KAKAO_UNLINK, responseException(HttpStatus.UNAUTHORIZED, ""));

        // then
        assertAll(
            () -> assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR),
            () -> assertThat(exception.getStatusCode()).isEqualTo(500),
            () -> assertThat(exception.isRetryable()).isFalse()
        );
    }

    @DisplayName("429는 RATE_LIMITED로 분류되어 재시도 대상이 된다")
    @Test
    void fromTooManyRequests() {
        // when
        ExternalApiException exception = ExternalApiException.fromStatus(
            ExternalOperation.KAKAO_UNLINK, responseException(HttpStatus.TOO_MANY_REQUESTS, ""));

        // then
        assertAll(
            () -> assertThat(exception.getType()).isEqualTo(FailureType.RATE_LIMITED),
            () -> assertThat(exception.isRetryable()).isTrue()
        );
    }

    @DisplayName("응답 본문을 보관하여 2차 세분화에서 파싱할 수 있게 한다")
    @Test
    void keepsResponseBody() {
        // when
        ExternalApiException exception = ExternalApiException.fromStatus(
            ExternalOperation.APPLE_TOKEN, responseException(HttpStatus.BAD_REQUEST, "{\"error\":\"invalid_grant\"}"));

        // then
        assertThat(exception.getResponseBody()).contains("invalid_grant");
    }

    @DisplayName("connect timeout과 read timeout을 구분한다")
    @Test
    void fromTimeout() {
        // when
        ExternalApiException connect = ExternalApiException.fromIo(
            ExternalOperation.APPLE_TOKEN, new ResourceAccessException("io", new SocketTimeoutException("Connect timed out")));
        ExternalApiException read = ExternalApiException.fromIo(
            ExternalOperation.APPLE_TOKEN, new ResourceAccessException("io", new SocketTimeoutException("Read timed out")));

        // then
        assertAll(
            () -> assertThat(connect.getType()).isEqualTo(FailureType.CONNECT_TIMEOUT),
            () -> assertThat(read.getType()).isEqualTo(FailureType.READ_TIMEOUT)
        );
    }

    @DisplayName("비멱등 연산의 read timeout은 재시도 대상이 아니다")
    @Test
    void readTimeoutOnNonIdempotentOperation() {
        // when
        ExternalApiException token = ExternalApiException.fromIo(
            ExternalOperation.APPLE_TOKEN, new ResourceAccessException("io", new SocketTimeoutException("Read timed out")));
        ExternalApiException revoke = ExternalApiException.fromIo(
            ExternalOperation.APPLE_REVOKE, new ResourceAccessException("io", new SocketTimeoutException("Read timed out")));

        // then
        assertAll(
            () -> assertThat(token.isRetryable()).isFalse(),
            () -> assertThat(revoke.isRetryable()).isTrue()
        );
    }

    @DisplayName("타임아웃이 아닌 IO 실패는 CONNECTION_FAILED로 분류된다")
    @Test
    void fromConnectionFailure() {
        // when
        ExternalApiException exception = ExternalApiException.fromIo(
            ExternalOperation.KAKAO_UNLINK, new ResourceAccessException("io", new IOException("Connection reset")));

        // then
        assertThat(exception.getType()).isEqualTo(FailureType.CONNECTION_FAILED);
    }

    @DisplayName("as()로 타입을 세분화해도 연산과 응답 본문은 유지된다")
    @Test
    void refineType() {
        // given
        ExternalApiException origin = ExternalApiException.fromStatus(
            ExternalOperation.APPLE_TOKEN, responseException(HttpStatus.BAD_REQUEST, "{\"error\":\"invalid_grant\"}"));

        // when
        ExternalApiException refined = origin.as(FailureType.AUTH_REJECTED);

        // then
        assertAll(
            () -> assertThat(refined.getType()).isEqualTo(FailureType.AUTH_REJECTED),
            () -> assertThat(refined.getStatusCode()).isEqualTo(401),
            () -> assertThat(refined.getResponseBody()).contains("invalid_grant"),
            () -> assertThat(refined.getOperation().operationName()).isEqualTo("token"),
            () -> assertThat(refined.getOperation().service()).isEqualTo(ExternalService.APPLE)
        );
    }

    private RestClientResponseException responseException(HttpStatus status, String body) {
        return new RestClientResponseException(
            "error", status.value(), status.getReasonPhrase(),
            HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
