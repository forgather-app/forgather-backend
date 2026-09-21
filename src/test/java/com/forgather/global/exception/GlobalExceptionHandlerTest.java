package com.forgather.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.exception.ExternalApiException;
import com.forgather.global.external.exception.ExternalFailureType;

import io.jsonwebtoken.JwtException;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThrowingController controller = new ThrowingController();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @BeforeEach
    void resetController() {
        controller.toThrow = null;
    }

    @Nested
    @DisplayName("Spring 표준 예외 핸들러")
    class SpringExceptionHandlers {

        @DisplayName("MethodArgumentNotValidException -> 400 / VALIDATION_FAILED")
        @Test
        void methodArgumentNotValid() throws Exception {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getMessage()).thenReturn("validation failed for field");
            when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(400));

            Snapshot res = perform(ex);

            assertEnvelope(res, 400, "VALIDATION_FAILED", "validation failed for field");
        }

        @DisplayName("HttpMediaTypeNotSupportedException -> 415 / UNSUPPORTED_MEDIA_TYPE")
        @Test
        void httpMediaTypeNotSupported() throws Exception {
            Snapshot res = perform(new HttpMediaTypeNotSupportedException("text/plain not supported"));

            assertAll(
                () -> assertThat(res.status).isEqualTo(415),
                () -> assertThat(res.code).isEqualTo("UNSUPPORTED_MEDIA_TYPE"),
                () -> assertThat(res.message).contains("text/plain not supported"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }

        @DisplayName("HttpRequestMethodNotSupportedException -> 405 / METHOD_NOT_ALLOWED")
        @Test
        void httpRequestMethodNotSupported() throws Exception {
            Snapshot res = perform(new HttpRequestMethodNotSupportedException("DELETE"));

            assertAll(
                () -> assertThat(res.status).isEqualTo(405),
                () -> assertThat(res.code).isEqualTo("METHOD_NOT_ALLOWED"),
                () -> assertThat(res.message).contains("DELETE"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }

        @DisplayName("NoResourceFoundException -> 404 / RESOURCE_NOT_FOUND")
        @Test
        void noResourceFound() throws Exception {
            Snapshot res = perform(new NoResourceFoundException(HttpMethod.GET, "/missing"));

            assertAll(
                () -> assertThat(res.status).isEqualTo(404),
                () -> assertThat(res.code).isEqualTo("RESOURCE_NOT_FOUND"),
                () -> assertThat(res.message).contains("/missing"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }

        @DisplayName("MissingRequestCookieException -> 400 / MISSING_COOKIE / 가공 메시지 보존")
        @Test
        void missingRequestCookie() throws Exception {
            MethodParameter parameter = sampleParameter();

            Snapshot res = perform(new MissingRequestCookieException("auth-token", parameter));

            assertEnvelope(res, 400, "MISSING_COOKIE", "필요한 쿠키가 누락되었습니다: auth-token");
        }

        @DisplayName("MethodArgumentTypeMismatchException -> 400 / BAD_REQUEST / 가공 메시지 보존")
        @Test
        void methodArgumentTypeMismatch() throws Exception {
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "id",
                sampleParameter(),
                new IllegalArgumentException("not a number")
            );

            Snapshot res = perform(ex);

            // parameterName은 -parameters 컴파일 옵션 유무에 따라 id/arg0/null로 달라지므로 패턴으로 검증한다
            assertAll(
                () -> assertThat(res.status).isEqualTo(400),
                () -> assertThat(res.code).isEqualTo("BAD_REQUEST"),
                () -> assertThat(res.message).matches("파라미터 '.*'의 타입이 올바르지 않습니다\\. 필요한 타입: Long"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }

        @DisplayName("PropertyReferenceException -> 400 / BAD_REQUEST / 가공 메시지 보존")
        @Test
        void propertyReference() throws Exception {
            PropertyReferenceException ex = new PropertyReferenceException(
                "nonexistent",
                TypeInformation.of(SampleHolder.class),
                List.of()
            );

            Snapshot res = perform(ex);

            assertEnvelope(res, 400, "BAD_REQUEST",
                "'SampleHolder' 타입에 'nonexistent' 속성이 존재하지 않습니다.");
        }

        @DisplayName("MultipartException -> 400 / BAD_REQUEST")
        @Test
        void multipart() throws Exception {
            Snapshot res = perform(new MultipartException("multipart parsing failed"));

            assertEnvelope(res, 400, "BAD_REQUEST", "multipart parsing failed");
        }

        @DisplayName("MaxUploadSizeExceededException -> 413 / PAYLOAD_TOO_LARGE")
        @Test
        void maxUploadSizeExceeded() throws Exception {
            Snapshot res = perform(new MaxUploadSizeExceededException(1024L));

            assertAll(
                () -> assertThat(res.status).isEqualTo(413),
                () -> assertThat(res.code).isEqualTo("PAYLOAD_TOO_LARGE"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }

        @DisplayName("io.jsonwebtoken.JwtException -> 401 / JWT_INVALID")
        @Test
        void jwt() throws Exception {
            Snapshot res = perform(new JwtException("invalid jwt"));

            assertEnvelope(res, 401, "JWT_INVALID", "invalid jwt");
        }
    }

    @Nested
    @DisplayName("커스텀 인증/인가/리소스 예외 (별도 핸들러로 매칭, BaseException 분기에 흡수되지 않음)")
    class CustomDomainHandlers {

        @DisplayName("NotFoundException -> 404 / NOT_FOUND")
        @Test
        void notFound() throws Exception {
            Snapshot res = perform(new NotFoundException("리소스를 찾을 수 없습니다"));

            assertEnvelope(res, 404, "NOT_FOUND", "리소스를 찾을 수 없습니다");
        }

        @DisplayName("ForbiddenException -> 403 / FORBIDDEN")
        @Test
        void forbidden() throws Exception {
            Snapshot res = perform(new ForbiddenException("접근 권한이 없습니다"));

            assertEnvelope(res, 403, "FORBIDDEN", "접근 권한이 없습니다");
        }

        @DisplayName("UnauthorizedException -> 401 / UNAUTHORIZED")
        @Test
        void unauthorized() throws Exception {
            Snapshot res = perform(new UnauthorizedException("로그인이 필요합니다"));

            assertEnvelope(res, 401, "UNAUTHORIZED", "로그인이 필요합니다");
        }

        @DisplayName("외부 장애(UPSTREAM_ERROR) -> 503 / EXTERNAL_API_UNAVAILABLE / 재시도 안내")
        @Test
        void externalUpstreamError() throws Exception {
            Snapshot res = perform(new ExternalApiException(
                ExternalOperation.APPLE_TOKEN, ExternalFailureType.UPSTREAM_ERROR, "Apple token 서버에 장애가 발생했습니다."));

            assertEnvelope(res, 503, "EXTERNAL_API_UNAVAILABLE",
                "외부 서비스 일시 장애로 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        @DisplayName("우리 쪽 결함(CALLER_ERROR) -> 500 / INTERNAL_ERROR / 메시지 마스킹")
        @Test
        void externalCallerError() throws Exception {
            Snapshot res = perform(new ExternalApiException(
                ExternalOperation.KAKAO_UNLINK, ExternalFailureType.CALLER_ERROR, "admin key가 유효하지 않습니다."));

            assertEnvelope(res, 500, "INTERNAL_ERROR", "예상치 못한 오류가 발생했습니다.");
        }

        @DisplayName("사용자 입력 문제(AUTH_REJECTED) -> 401 / UNAUTHORIZED / 재로그인 안내")
        @Test
        void externalAuthRejected() throws Exception {
            Snapshot res = perform(new ExternalApiException(
                ExternalOperation.APPLE_TOKEN, ExternalFailureType.AUTH_REJECTED, "Apple authorization code가 유효하지 않습니다."));

            assertEnvelope(res, 401, "UNAUTHORIZED", "소셜 로그인 인증에 실패했습니다. 다시 로그인해 주세요.");
        }
    }

    @Nested
    @DisplayName("BaseException 분기 (handleBaseException + resolveCode + maskServerMessage)")
    class BaseExceptionBranch {

        @DisplayName("ConflictException -> 409 / CONFLICT (4xx 메시지 그대로)")
        @Test
        void conflict() throws Exception {
            Snapshot res = perform(new ConflictException("이미 신고된 방명록입니다"));

            assertEnvelope(res, 409, "CONFLICT", "이미 신고된 방명록입니다");
        }

        @DisplayName("JwtBaseException(401) -> 401 / JWT_INVALID (instanceof 우선)")
        @Test
        void jwtBase() throws Exception {
            Snapshot res = perform(new JwtParseException("토큰 파싱 실패", HttpStatus.UNAUTHORIZED));

            assertEnvelope(res, 401, "JWT_INVALID", "토큰 파싱 실패");
        }

        @DisplayName("FileDownloadException(500) -> 500 / FILE_DOWNLOAD_FAILED / 영역별 마스킹 메시지")
        @Test
        void fileDownload_5xxMasked() throws Exception {
            Snapshot res = perform(new FileDownloadException("S3 GetObject failed: presignedUrl=..."));

            assertEnvelope(res, 500, "FILE_DOWNLOAD_FAILED", "파일 다운로드 처리 중 오류가 발생했습니다.");
        }

        @DisplayName("BaseNullPointerException(500) -> 500 / INTERNAL_ERROR / 기본 마스킹 메시지")
        @Test
        void baseNullPointer_5xxMasked() throws Exception {
            Snapshot res = perform(
                new BaseNullPointerException("Cannot invoke User.getId() because this.user is null"));

            assertEnvelope(res, 500, "INTERNAL_ERROR", "예상치 못한 오류가 발생했습니다.");
        }

        @DisplayName("BaseException 직접 throw (default 400) -> 400 / BAD_REQUEST")
        @Test
        void baseException_default400() throws Exception {
            Snapshot res = perform(new BaseException("작품명은 공백만 입력할 수 없습니다."));

            assertEnvelope(res, 400, "BAD_REQUEST", "작품명은 공백만 입력할 수 없습니다.");
        }

        @DisplayName("BaseException(401) 직접 throw -> 401 / UNAUTHORIZED (status와 code 일치)")
        @Test
        void baseException_401() throws Exception {
            Snapshot res = perform(new BaseException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED));

            assertEnvelope(res, 401, "UNAUTHORIZED", "인증이 필요합니다.");
        }

        @DisplayName("BaseException(403) 직접 throw -> 403 / FORBIDDEN (status와 code 일치)")
        @Test
        void baseException_403() throws Exception {
            Snapshot res = perform(new BaseException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN));

            assertEnvelope(res, 403, "FORBIDDEN", "접근 권한이 없습니다.");
        }

        @DisplayName("BaseException(404) 직접 throw -> 404 / NOT_FOUND (status와 code 일치)")
        @Test
        void baseException_404() throws Exception {
            Snapshot res = perform(new BaseException("리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

            assertEnvelope(res, 404, "NOT_FOUND", "리소스를 찾을 수 없습니다.");
        }

        @DisplayName("BaseException(405) 직접 throw -> 405 / METHOD_NOT_ALLOWED")
        @Test
        void baseException_405() throws Exception {
            Snapshot res = perform(new BaseException("허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED));

            assertEnvelope(res, 405, "METHOD_NOT_ALLOWED", "허용되지 않은 메서드입니다.");
        }

        @DisplayName("BaseException(413) 직접 throw -> 413 / PAYLOAD_TOO_LARGE")
        @Test
        void baseException_413() throws Exception {
            Snapshot res = perform(new BaseException("요청 본문이 너무 큽니다.", HttpStatus.PAYLOAD_TOO_LARGE));

            assertEnvelope(res, 413, "PAYLOAD_TOO_LARGE", "요청 본문이 너무 큽니다.");
        }

        @DisplayName("BaseException(415) 직접 throw -> 415 / UNSUPPORTED_MEDIA_TYPE")
        @Test
        void baseException_415() throws Exception {
            Snapshot res = perform(new BaseException("지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE));

            assertEnvelope(res, 415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다.");
        }
    }

    @Nested
    @DisplayName("Exception fallback")
    class FallbackHandler {

        @DisplayName("RuntimeException -> 500 / INTERNAL_ERROR / 마스킹 메시지 (원본 메시지 노출 차단)")
        @Test
        void runtime_masked() throws Exception {
            Snapshot res = perform(new IllegalStateException("DB connection refused: jdbc:mysql://prod-host:3306"));

            assertAll(
                () -> assertThat(res.status).isEqualTo(500),
                () -> assertThat(res.code).isEqualTo("INTERNAL_ERROR"),
                () -> assertThat(res.message).isEqualTo("예상치 못한 오류가 발생했습니다."),
                () -> assertThat(res.message).doesNotContain("jdbc:mysql"),
                () -> assertThat(res.dataIsNull).isTrue()
            );
        }
    }

    private Snapshot perform(Exception toThrow) throws Exception {
        controller.toThrow = toThrow;
        MvcResult mvc = mockMvc.perform(get("/throw")).andReturn();
        JsonNode node = objectMapper.readTree(mvc.getResponse().getContentAsString());
        return new Snapshot(
            mvc.getResponse().getStatus(),
            node.get("code").asText(),
            node.get("message").asText(),
            node.get("data").isNull()
        );
    }

    private void assertEnvelope(Snapshot res, int status, String code, String message) {
        assertAll(
            () -> assertThat(res.status).isEqualTo(status),
            () -> assertThat(res.code).isEqualTo(code),
            () -> assertThat(res.message).isEqualTo(message),
            () -> assertThat(res.dataIsNull).isTrue()
        );
    }

    private MethodParameter sampleParameter() throws Exception {
        return new MethodParameter(SampleHolder.class.getDeclaredMethod("sample", Long.class), 0);
    }

    private record Snapshot(int status, String code, String message, boolean dataIsNull) {
    }

    private static class SampleHolder {
        public void sample(Long id) {
        }
    }

    @RestController
    static class ThrowingController {
        Exception toThrow;

        @GetMapping("/throw")
        public void throwIt() throws Exception {
            throw toThrow;
        }
    }
}
