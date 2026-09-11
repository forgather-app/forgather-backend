package com.forgather.domain.host.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.domain.host.service.HostAnonymizeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class HostWithdrawScheduler {

    private final HostAnonymizeService hostAnonymizeService;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 실행
    public void anonymizeExpiredHosts() {
        try {
            int anonymizedCount = hostAnonymizeService.anonymizeExpiredHosts();
            log.info("탈퇴 회원 익명화 완료. 처리: {}건", anonymizedCount);
        } catch (Exception e) {
            log.error("탈퇴 회원 익명화 배치 실패", e);
        }
    }
}
