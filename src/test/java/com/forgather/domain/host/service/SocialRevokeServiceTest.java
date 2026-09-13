package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxType;

@ExtendWith(MockitoExtension.class)
class SocialRevokeServiceTest {

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private AppleApiClient appleApiClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private KakaoHostRepository kakaoHostRepository;

    @Mock
    private AppleHostRepository appleHostRepository;

    @Mock
    private KakaoHost kakaoHost;

    @Mock
    private AppleHost appleHost;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocialRevokeService createProcessor() {
        return new SocialRevokeService(
            kakaoApiClient, appleApiClient, outboxService, kakaoHostRepository, appleHostRepository, objectMapper);
    }

    /**
     * payload는 JSON 문자열로 저장된다.
     */
    private Outbox pendingOutbox(Long id, SocialRevokePayload payload) throws JsonProcessingException {
        return pendingOutbox(id, objectMapper.writeValueAsString(payload));
    }

    private Outbox pendingOutbox(Long id, String payload) {
        Outbox outbox = Outbox.pending(OutboxType.SOCIAL_REVOKE, payload);
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }

    @DisplayName("Kakao 연결 해제에 성공하면 outbox를 완료 처리한다")
    @Test
    void processKakaoSuccess() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));

        // when
        SocialRevokeResult result = createProcessor().process();

        // then
        verify(kakaoApiClient).unlink("kakao-user-1");
        verify(outboxService).complete(1L);
        verify(outboxService, never()).increaseFailCount(1L);
        assertAll(
            () -> assertThat(result.succeededCount()).isEqualTo(1),
            () -> assertThat(result.failedCount()).isZero()
        );
    }

    @DisplayName("Apple 연결 해제에 성공하면 outbox를 완료 처리한다")
    @Test
    void processAppleSuccess() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.APPLE, "apple-user-1", "apple-refresh-token"));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));

        // when
        createProcessor().process();

        // then
        verify(appleApiClient).revoke("apple-refresh-token");
        verify(outboxService).complete(1L);
    }

    @DisplayName("같은 Kakao 계정으로 재가입한 상태면 연결 해제 대신 outbox를 취소 처리한다")
    @Test
    void processKakaoCancelsWhenReRegistered() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));
        when(kakaoHostRepository.findByUserId("kakao-user-1")).thenReturn(Optional.of(kakaoHost));
        when(outboxService.cancel(1L)).thenReturn(true);

        // when
        SocialRevokeResult result = createProcessor().process();

        // then
        verify(kakaoApiClient, never()).unlink("kakao-user-1");
        verify(outboxService).cancel(1L);
        verify(outboxService, never()).complete(1L);
        assertAll(
            () -> assertThat(result.succeededCount()).isZero(),
            () -> assertThat(result.failedCount()).isZero(),
            () -> assertThat(result.canceledCount()).isEqualTo(1)
        );
    }

    @DisplayName("같은 Apple 계정으로 재가입한 상태면 연결 해제 대신 outbox를 취소 처리한다")
    @Test
    void processAppleCancelsWhenReRegistered() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.APPLE, "apple-user-1", "apple-refresh-token"));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));
        when(appleHostRepository.findByUserId("apple-user-1")).thenReturn(Optional.of(appleHost));
        when(outboxService.cancel(1L)).thenReturn(true);

        // when
        SocialRevokeResult result = createProcessor().process();

        // then
        verify(appleApiClient, never()).revoke("apple-refresh-token");
        verify(outboxService).cancel(1L);
        assertThat(result.canceledCount()).isEqualTo(1);
    }

    @DisplayName("연결 해제에 실패하면 예외를 전파하지 않고 실패 횟수만 증가시킨다")
    @Test
    void processFailure() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        SocialRevokeResult result = createProcessor().process();

        // then
        verify(outboxService).increaseFailCount(1L);
        verify(outboxService, never()).complete(1L);
        assertAll(
            () -> assertThat(result.succeededCount()).isZero(),
            () -> assertThat(result.failedCount()).isEqualTo(1)
        );
    }

    @DisplayName("한 건이 실패해도 나머지 건은 계속 처리한다")
    @Test
    void processContinuesAfterFailure() throws JsonProcessingException {
        // given
        Outbox failing = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        Outbox succeeding = pendingOutbox(2L,
            new SocialRevokePayload(2L, SocialProvider.APPLE, "apple-user-1", "apple-refresh-token"));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE))
            .thenReturn(List.of(failing, succeeding));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        SocialRevokeResult result = createProcessor().process();

        // then
        verify(outboxService).increaseFailCount(1L);
        verify(outboxService).complete(2L);
        assertAll(
            () -> assertThat(result.succeededCount()).isEqualTo(1),
            () -> assertThat(result.failedCount()).isEqualTo(1)
        );
    }

    @DisplayName("모든 건을 처리한 뒤 실패 상한에 도달한 outbox를 FAILED로 전환한다")
    @Test
    void processFailsExhaustedAfterAll() throws JsonProcessingException {
        // given
        Outbox outbox = pendingOutbox(1L,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        when(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).thenReturn(List.of(outbox));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        createProcessor().process();

        // then
        InOrder inOrder = Mockito.inOrder(outboxService);
        inOrder.verify(outboxService).increaseFailCount(1L);
        inOrder.verify(outboxService).failExhausted(OutboxType.SOCIAL_REVOKE, SocialRevokeService.MAX_FAIL_COUNT);
    }
}
