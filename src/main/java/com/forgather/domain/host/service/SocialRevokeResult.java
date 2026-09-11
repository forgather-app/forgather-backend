package com.forgather.domain.host.service;

public record SocialRevokeResult(int succeededCount, int failedCount) {

    public boolean isEmpty() {
        return succeededCount == 0 && failedCount == 0;
    }
}
