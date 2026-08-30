package com.forgather.domain.host.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.domain.host.service.WithdrawnHostPurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnHostPurgeScheduler {

    private final WithdrawnHostPurgeService withdrawnHostPurgeService;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 실행
    public void purgeWithdrawnHosts() {
        try {
            withdrawnHostPurgeService.purgeExpiredHosts();
        } catch (Exception e) {
            log.error("탈퇴 회원 purge 배치 실패", e);
        }
    }
}
