package com.forgather.global.outbox;

public record OutboxProcessResult(
    int succeededCount,
    int failedCount,
    int canceledCount
) {

    public boolean isEmpty() {
        return succeededCount == 0 && failedCount == 0 && canceledCount == 0;
    }
}
