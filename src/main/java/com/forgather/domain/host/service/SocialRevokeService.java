package com.forgather.domain.host.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
public class SocialRevokeService {

    private final KakaoApiClient kakaoApiClient;
    private final AppleApiClient appleApiClient;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * TODO
     * 실패 상한선
     *
     * 외부 API 호출 동안 DB 커넥션을 점유하지 않도록 트랜잭션을 두지 않는다.
     * outbox 상태 변경은 OutboxService의 건별 트랜잭션에서 처리한다.
     */
    public SocialRevokeResult process() {
        List<Outbox> outboxes = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);

        int succeededCount = 0;
        int failedCount = 0;
        for (Outbox outbox : outboxes) {
            SocialRevokePayload payload;
            try {
                payload = objectMapper.readValue(outbox.getPayload(), SocialRevokePayload.class);
            } catch (Exception e) {
                outboxService.increaseFailCount(outbox.getId());
                failedCount++;
                log.warn("outbox payload 변환 실패. outboxId: {}", outbox.getId(), e);
                continue;
            }

            try {
                process(payload);
                outboxService.complete(outbox.getId());
                succeededCount++;
            } catch (Exception e) {
                outboxService.increaseFailCount(outbox.getId());
                failedCount++;
                log.warn("소셜 연결 해제 실패. outboxId: {}, hostId: {}, provider: {}",
                    outbox.getId(), payload.hostId(), payload.provider(), e);
            }
        }

        return new SocialRevokeResult(succeededCount, failedCount);
    }

    private void process(SocialRevokePayload payload) {
        switch (payload.provider()) {
            case KAKAO -> kakaoApiClient.unlink(payload.userId());
            case APPLE -> appleApiClient.revoke(payload.refreshToken());
            default -> throw new BaseException(
                "지원하지 않는 provider입니다. provider: " + payload.provider(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
