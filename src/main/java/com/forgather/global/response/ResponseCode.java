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
    ;
}
