package com.forgather.global.outbox;

public interface OutboxHandler {

    boolean supports(OutboxType type);

    OutboxTaskResult process(Outbox outbox);

    long getLeaseDurationSeconds();

    int getMaxFailCount();
}
