package com.forgather.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 외부 서비스(Apple, Kakao 등) 호출이 일시적으로 실패했음을 나타낸다.
 * 우리 서버 결함이 아니므로 error가 아닌 warn으로 기록한다.
 * nginx가 내는 502(앱 다운)·504(앱 응답 지연)와 겹치지 않도록 503을 사용한다.
 */
public class ExternalApiException extends BaseException {

    public ExternalApiException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
