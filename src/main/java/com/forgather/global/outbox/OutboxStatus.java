package com.forgather.global.outbox;

/**
 * outbox 작업의 상태. 타입 특화 결과는 담지 않고 공용 어휘만 유지한다.
 */
public enum OutboxStatus {

    /** 처리 대기 중. 스케줄러 폴링 대상 */
    PENDING,

    /** 처리 완료 (이미 처리된 것으로 확인된 경우 포함) */
    COMPLETED,

    /** 실행 전 가드에 걸려 더 이상 수행할 필요가 없어진 경우 */
    CANCELED,

    /** 영구 실패 또는 재시도 상한 도달 */
    FAILED,
    ;
}
