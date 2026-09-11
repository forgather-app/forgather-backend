package com.forgather.domain.host.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialRevokeProcessor {

    private final KakaoApiClient kakaoApiClient;
    private final AppleApiClient appleApiClient;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * TODO
     * 실패 상한선
     */
    @Transactional
    public SocialRevokeResult process() {
        List<Outbox> outboxes = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);

        int succeededCount = 0;
        int failedCount = 0;
        for (Outbox outbox : outboxes) {
            SocialRevokePayload command;
            try {
                command = objectMapper.convertValue(outbox.getPayload(), SocialRevokePayload.class);
            } catch (Exception e) {
                outbox.increaseFailCount();
                failedCount++;
                log.warn("outbox payload 변환 실패. outboxId: {}", outbox.getId(), e);
                continue;
            }

            try {
                switch (command.provider()) {
                    case KAKAO -> kakaoApiClient.unlink(command.userId());
                    case APPLE -> appleApiClient.revoke(command.refreshToken());
                    default -> throw new BaseException(
                        "지원하지 않는 provider입니다. provider: " + command.provider(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                outbox.complete();
                succeededCount++;
            } catch (Exception e) {
                outbox.increaseFailCount();
                failedCount++;
                log.warn("소셜 연결 해제 실패. outboxId: {}, hostId: {}, provider: {}, failCount: {}",
                    outbox.getId(), command.hostId(), command.provider(), outbox.getFailCount(), e);
            }
        }

        return new SocialRevokeResult(succeededCount, failedCount);
    }
}
