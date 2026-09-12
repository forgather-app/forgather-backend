package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxStatus;
import com.forgather.global.outbox.OutboxType;

@ExtendWith(MockitoExtension.class)
class SocialRevokeProcessorTest {

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private AppleApiClient appleApiClient;

    @Mock
    private OutboxService outboxService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocialRevokeProcessor createProcessor() {
        return new SocialRevokeProcessor(kakaoApiClient, appleApiClient, outboxService, objectMapper);
    }

    /**
     * payload는 JSON 문자열로 저장된다.
     */
    private Outbox pendingOutbox(SocialRevokePayload payload) throws JsonProcessingException {
        return Outbox.pending(OutboxType.SOCIAL_REVOKE, objectMapper.writeValueAsString(payload));
    }

    @DisplayName("Kakao 연결 해제에 성공하면 outbox를 완료 처리한다")
    @Test
    void processKakaoSuccess() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));

        // when
        createProcessor().process();

        // then
        verify(kakaoApiClient).unlink("kakao-user-1");
        assertAll(
            () -> assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.COMPLETED),
            () -> assertThat(outbox.getFailCount()).isZero()
        );
    }

    @DisplayName("Apple 연결 해제에 성공하면 outbox를 완료 처리한다")
    @Test
    void processAppleSuccess() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(
            new SocialRevokePayload(1L, SocialProvider.APPLE, "apple-user-1", "apple-refresh-token"));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));

        // when
        createProcessor().process();

        // then
        verify(appleApiClient).revoke("apple-refresh-token");
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
    }

    @DisplayName("연결 해제에 실패하면 예외를 전파하지 않고 실패 횟수만 증가시킨다")
    @Test
    void processFailure() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        createProcessor().process();

        // then
        assertAll(
            () -> assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(outbox.getFailCount()).isEqualTo(1)
        );
    }

    @DisplayName("한 건이 실패해도 나머지 건은 계속 처리한다")
    @Test
    void processContinuesAfterFailure() throws JsonProcessingException {
        // given
        Outbox failing = pendingOutbox(
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        Outbox succeeding = pendingOutbox(
            new SocialRevokePayload(2L, SocialProvider.APPLE, "apple-user-1", "apple-refresh-token"));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE))
            .thenReturn(List.of(failing, succeeding));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        createProcessor().process();

        // then
        assertAll(
            () -> assertThat(failing.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(succeeding.getStatus()).isEqualTo(OutboxStatus.COMPLETED)
        );
    }
}
