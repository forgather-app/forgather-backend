package com.forgather.global.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * 예측 가능한 클라이언트발 예외 -> info
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.VALIDATION_FAILED, e.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException e
    ) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e
    ) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.METHOD_NOT_ALLOWED, e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.RESOURCE_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestCookieException(MissingRequestCookieException e) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.MISSING_COOKIE, "필요한 쿠키가 누락되었습니다: " + e.getCookieName()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException e) {
        logClientInfo(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.NOT_FOUND, e.getMessage()));
    }

    // 컨트롤러 요청 파라미터의 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e
    ) {
        logClientWarning(e);
        String parameterName = e.getParameter().getParameterName();
        String requiredType = e.getRequiredType().getSimpleName();
        String message = String.format("파라미터 '%s'의 타입이 올바르지 않습니다. 필요한 타입: %s", parameterName, requiredType);
        return ResponseEntity.status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.BAD_REQUEST, message));
    }

    // 요청 본문 역직렬화 실패 (잘못된 enum 값, 타입 불일치, JSON 구문 오류 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logClientWarning(e);
        String message = resolveNotReadableMessage(e);
        return ResponseEntity.status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.BAD_REQUEST, message));
    }

    private String resolveNotReadableMessage(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String typeName = ife.getTargetType().getSimpleName();
            return "'%s' 형식이 올바르지 않습니다.".formatted(typeName);
        }
        return "요청 본문 형식이 올바르지 않습니다.";
    }

    // 클라이언트가 요청에 기재한 속성 존재하지 않을 경우 ex) 페이지네이션 sort 조건 속성
    // 서버에서 잘못된 코드를 작성해서 이 예외가 발생할 수도 있지만 여기까지 도달하지 않을 것이라 판단 ex) 잘못된 jpa 쿼리 메서드명
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handlePropertyReferenceException(PropertyReferenceException e) {
        logClientWarning(e);
        String propertyName = e.getPropertyName();
        String simpleTypeName = e.getType().getType().getSimpleName();
        return ResponseEntity.status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(
                ResponseCode.BAD_REQUEST,
                "'%s' 타입에 '%s' 속성이 존재하지 않습니다.".formatted(simpleTypeName, propertyName)
            ));
    }

    /**
     * 예측 가능하지만 주의해야할 예외 -> warn
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e) {
        logClientWarning(e);
        return ResponseEntity.status(BAD_REQUEST)
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logClientWarning(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.PAYLOAD_TOO_LARGE, e.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException e) {
        logClientWarning(e);
        return ResponseEntity.status(UNAUTHORIZED)
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.JWT_INVALID, e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException e) {
        logClientWarning(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        logClientWarning(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, e.getMessage()));
    }

    /**
     * 외부 서비스 일시 장애 -> warn
     * 우리 서버 결함이 아니므로 error로 올리지 않되, 원인 추적을 위해 stack trace는 남긴다.
     * 응답은 5xx 마스킹 대신 재시도를 안내하는 고정 문구를 쓴다.
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApiException(ExternalApiException e) {
        logExternalApiWarning(e);
        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(
                ResponseCode.EXTERNAL_API_UNAVAILABLE,
                "외부 서비스 일시 장애로 요청에 실패했습니다. 잠시 후 다시 시도해 주세요."));
    }

    /**
     * 핸들링 되지 않은 커스텀 비즈니스 예외
     * 4XX -> info
     * 5XX -> error
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        if (e.isSecurityError()) {
            logClientWarning(e);
        } else if (e.isClientError()) {
            logClientInfo(e);
        } else {
            logServerError(e);
        }

        ResponseCode responseCode = resolveCode(e);
        String message = getResponseMessage(e, responseCode);

        return ResponseEntity.status(e.getStatusCode())
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(responseCode, message));
    }

    private ResponseCode resolveCode(BaseException e) {
        if (e instanceof JwtBaseException) {
            return ResponseCode.JWT_INVALID;
        }
        if (e instanceof FileDownloadException) {
            return ResponseCode.FILE_DOWNLOAD_FAILED;
        }
        int status = e.getStatusCode();
        if (status >= INTERNAL_SERVER_ERROR.value()) {
            return ResponseCode.INTERNAL_ERROR;
        }
        return resolveClientErrorCode(status);
    }

    /**
     * BaseException(message, HttpStatus) 형태로 4xx status를 직접 받은 경로에서
     * isSecurityError() 등 status 기반 판단과 응답 code가 어긋나지 않도록 분기
     */
    private ResponseCode resolveClientErrorCode(int status) {
        return switch (HttpStatus.valueOf(status)) {
            case UNAUTHORIZED -> ResponseCode.UNAUTHORIZED;
            case FORBIDDEN -> ResponseCode.FORBIDDEN;
            case NOT_FOUND -> ResponseCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ResponseCode.METHOD_NOT_ALLOWED;
            case CONFLICT -> ResponseCode.CONFLICT;
            case PAYLOAD_TOO_LARGE -> ResponseCode.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> ResponseCode.UNSUPPORTED_MEDIA_TYPE;
            default -> ResponseCode.BAD_REQUEST;
        };
    }

    private String getResponseMessage(BaseException exception, ResponseCode responseCode) {
        // 서버 에러의 경우 API 응답 메시지 마스킹
        if (exception.getStatusCode() >= INTERNAL_SERVER_ERROR.value()) {
            return maskServerMessage(responseCode);
        }
        return exception.getMessage();
    }

    private String maskServerMessage(ResponseCode code) {
        return switch (code) {
            case FILE_UPLOAD_FAILED -> "파일 업로드 처리 중 오류가 발생했습니다.";
            case FILE_DOWNLOAD_FAILED -> "파일 다운로드 처리 중 오류가 발생했습니다.";
            default -> "예상치 못한 오류가 발생했습니다.";
        };
    }

    private void logClientInfo(Exception e) {
        log.atInfo().log("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    private void logClientWarning(Exception e) {
        log.atWarn().log("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * 핸들링 되지 않은 모든 예외 -> error
     * 원본 메시지는 내부 정보(스키마/경로/라이브러리 내부 구조 등) 유출 우려가 있어 응답에서는 마스킹하고
     * 추적은 logServerError가 남기는 stack trace로 수행한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        logServerError(e);
        return ResponseEntity.internalServerError()
            .contentType(APPLICATION_JSON)
            .body(ApiResponse.error(ResponseCode.INTERNAL_ERROR, "예상치 못한 오류가 발생했습니다."));
    }

    private void logServerError(Exception e) {
        log.atError().setCause(e).log("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    private void logExternalApiWarning(Exception e) {
        log.atWarn().setCause(e).log("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }
}
