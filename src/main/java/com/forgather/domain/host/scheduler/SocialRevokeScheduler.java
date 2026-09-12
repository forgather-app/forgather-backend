package com.forgather.domain.host.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.domain.host.service.SocialRevokeResult;
import com.forgather.domain.host.service.SocialRevokeProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class SocialRevokeScheduler {

    private final SocialRevokeProcessor socialRevokeProcessor;

    @Scheduled(cron = "0 */1 * * * *")
    public void processPendingRevokes() {
        try {
            SocialRevokeResult result = socialRevokeProcessor.process();
            if (result.isEmpty()) {
                return;
            }
            log.info("소셜 연결 해제 완료. 성공: {}건, 실패: {}건", result.succeededCount(), result.failedCount());
        } catch (Exception e) {
            log.error("소셜 연결 해제 배치 실패", e);
        }
    }
}
