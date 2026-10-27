package com.forgather.global.outbox;

public enum OutboxStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,
    ;
}
