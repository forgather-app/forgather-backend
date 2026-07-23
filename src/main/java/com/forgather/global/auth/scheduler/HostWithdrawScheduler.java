package com.forgather.global.auth.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.global.auth.service.HostAnonymizeService;
import com.forgather.global.auth.service.SocialRevokeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HostWithdrawScheduler {

    private final HostAnonymizeService hostAnonymizeService;
    private final SocialRevokeService socialRevokeService;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 실행
    public void processWithdrawnHosts() {
        anonymizeExpiredHosts();
        retryFailedRevokes();
    }

    private void anonymizeExpiredHosts() {
        try {
            int anonymizedCount = hostAnonymizeService.anonymizeExpiredHosts();
            log.info("탈퇴 회원 익명화 완료. 처리: {}건", anonymizedCount);
        } catch (Exception e) {
            log.error("탈퇴 회원 익명화 배치 실패", e);
        }
    }

    private void retryFailedRevokes() {
        try {
            socialRevokeService.retryFailedRevokes();
        } catch (Exception e) {
            log.error("소셜 연결 해제 재시도 배치 실패", e);
        }
    }
}
