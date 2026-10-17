package com.forgather.global.exception;

import org.springframework.http.HttpStatus;

public class UpgradeRequiredException extends BaseException {

    public UpgradeRequiredException(String message) {
        super(message, HttpStatus.UPGRADE_REQUIRED);
    }
}
