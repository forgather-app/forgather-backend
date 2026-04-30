package com.forgather.global.response;

public enum ResponseCode {
    // 성공 응답
    SUCCESS,

    // 요청 오류 응답
    BAD_REQUEST,
    VALIDATION_FAILED,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    MISSING_COOKIE,
    PAYLOAD_TOO_LARGE,

    // 인증/인가 응답
    UNAUTHORIZED,
    FORBIDDEN,
    JWT_INVALID,

    // 리소스 응답
    NOT_FOUND,
    RESOURCE_NOT_FOUND,
    ;
}
