package com.forgather.domain.host.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
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

    public static final int MAX_FAIL_COUNT = 5;

    private final KakaoApiClient kakaoApiClient;
    private final AppleApiClient appleApiClient;
    private final OutboxService outboxService;
    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final ObjectMapper objectMapper;

    public SocialRevokeResult process() {
        List<Outbox> outboxes = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);

        int succeededCount = 0;
        int failedCount = 0;
        int canceledCount = 0;
        int retryableFailedCount = 0;
        for (Outbox outbox : outboxes) {
            SocialRevokePayload payload;
            try {
                payload = objectMapper.readValue(outbox.getPayload(), SocialRevokePayload.class);
                Objects.requireNonNull(payload);
            } catch (Exception e) {
                outboxService.fail(outbox.getId());
                failedCount++;
                log.warn("outbox payload 변환 실패. outboxId: {}", outbox.getId(), e);
                continue;
            }

            if (isSocialHostRegistered(payload)) {
                if (outboxService.cancel(outbox.getId())) {
                    canceledCount++;
                    log.info("같은 소셜 계정으로 재가입해 연결 해제를 취소합니다. outboxId: {}, hostId: {}, provider: {}",
                        outbox.getId(), payload.hostId(), payload.provider());
                }
                continue;
            }

            try {
                process(payload);
                outboxService.complete(outbox.getId());
                succeededCount++;
            } catch (Exception e) {
                outboxService.increaseFailCount(outbox.getId());
                failedCount++;
                retryableFailedCount++;
                log.warn("소셜 연결 해제 실패. outboxId: {}, hostId: {}, provider: {}",
                    outbox.getId(), payload.hostId(), payload.provider(), e);
            }
        }

        if (retryableFailedCount > 0) {
            failExhausted();
        }
        return new SocialRevokeResult(succeededCount, failedCount, canceledCount);
    }

    private boolean isSocialHostRegistered(SocialRevokePayload payload) {
        return switch (payload.provider()) {
            case KAKAO -> kakaoHostRepository.findByUserId(payload.userId()).isPresent();
            case APPLE -> appleHostRepository.findByUserId(payload.userId()).isPresent();
            default -> false;
        };
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

    private void failExhausted() {
        int exhaustedCount = outboxService.failExhausted(OutboxType.SOCIAL_REVOKE, MAX_FAIL_COUNT);
        if (exhaustedCount > 0) {
            log.error("실패 상한에 도달해 FAILED로 전환된 소셜 연결 해제 작업: {}건", exhaustedCount);
        }
    }
}
