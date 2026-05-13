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
    CONFLICT,

    // 파일 처리 응답
    FILE_UPLOAD_FAILED,
    FILE_DOWNLOAD_FAILED,
    S3_UNAVAILABLE,

    // 서버 오류 응답
    INTERNAL_ERROR,
    ;
}
