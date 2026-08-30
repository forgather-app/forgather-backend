package com.forgather.global.auth.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.global.auth.service.SocialRevokeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialRevokeRetryScheduler {

    private final SocialRevokeService socialRevokeService;

    @Scheduled(cron = "0 30 * * * *") // 매시 30분 실행 (새벽 4시 정각의 탈퇴 회원 purge 배치와 겹치지 않게)
    public void retryFailedRevokes() {
        try {
            socialRevokeService.retryFailedRevokes();
        } catch (Exception e) {
            log.error("소셜 연결 해제 재시도 배치 실패", e);
        }
    }
}
