package com.forgather.global.exception;

import org.springframework.http.HttpStatus;

public class InvalidConfigurationException extends BaseException {

    public InvalidConfigurationException(String configurationKeys) {
        super("필수 설정이 누락되었거나 올바르지 않습니다. 설정 키: %s".formatted(configurationKeys),
            HttpStatus.SERVICE_UNAVAILABLE);
    }
}
