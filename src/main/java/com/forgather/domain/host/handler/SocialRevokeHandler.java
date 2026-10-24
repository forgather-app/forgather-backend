package com.forgather.domain.host.handler;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.host.service.SocialRevokePayload;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxHandler;
import com.forgather.global.outbox.OutboxTaskResult;
import com.forgather.global.outbox.OutboxType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialRevokeHandler implements OutboxHandler {

    private final KakaoApiClient kakaoApiClient;
    private final AppleApiClient appleApiClient;
    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxType type) {
        return type == OutboxType.SOCIAL_REVOKE;
    }

    @Override
    public OutboxTaskResult process(Outbox outbox) {
        SocialRevokePayload payload;
        try {
            payload = objectMapper.readValue(outbox.getPayload(), SocialRevokePayload.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("소셜 연결 해제 payload 변환 실패. outboxId: {}", outbox.getId());
            return OutboxTaskResult.FAILED;
        }
        if (payload == null || payload.provider() == null) {
            log.warn("소셜 연결 해제 payload 또는 provider가 없습니다. outboxId: {}", outbox.getId());
            return OutboxTaskResult.FAILED;
        }

        if (isSocialHostRegistered(payload)) {
            log.info("같은 소셜 계정으로 재가입해 연결 해제를 취소합니다. outboxId: {}, hostId: {}, provider: {}",
                outbox.getId(), payload.hostId(), payload.provider());
            return OutboxTaskResult.CANCELED;
        }

        switch (payload.provider()) {
            case KAKAO -> kakaoApiClient.unlink(payload.userId());
            case APPLE -> appleApiClient.revoke(payload.refreshToken());
            default -> throw new BaseException(
                "지원하지 않는 provider입니다. provider: " + payload.provider(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return OutboxTaskResult.COMPLETED;
    }

    private boolean isSocialHostRegistered(SocialRevokePayload payload) {
        return switch (payload.provider()) {
            case KAKAO -> kakaoHostRepository.findByUserId(payload.userId()).isPresent();
            case APPLE -> appleHostRepository.findByUserId(payload.userId()).isPresent();
            default -> false;
        };
    }

    @Override
    public long getLeaseDurationSeconds() {
        return 20;
    }

    @Override
    public int getMaxFailCount() {
        return 5;
    }
}
