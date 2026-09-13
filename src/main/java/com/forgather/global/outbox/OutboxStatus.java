package com.forgather.global.outbox;

/**
 * outbox 작업의 상태. 타입 특화 결과는 담지 않고 공용 어휘만 유지한다.
 */
public enum OutboxStatus {

    PENDING,
    COMPLETED,
    FAILED,
    CANCELED,
    ;
}
