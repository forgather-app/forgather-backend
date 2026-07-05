package com.forgather.global.auth.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.global.auth.client.SocialAuthClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialPublicKeyScheduler {

    private final SocialAuthClient socialAuthClient;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    public void updateSocialPublicKeys() {
        log.info("jwks 캐시 갱신 시작");
        socialAuthClient.updateAllKeys();
        log.info("jwks 캐시 갱신 완료");
    }
}
