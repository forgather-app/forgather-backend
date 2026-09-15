package com.forgather.domain.host.service;

public record SocialRevokeResult(int succeededCount, int failedCount, int canceledCount) {

    public boolean isEmpty() {
        return succeededCount == 0 && failedCount == 0 && canceledCount == 0;
    }
}
