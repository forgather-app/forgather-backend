package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxRepository;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxStatus;
import com.forgather.global.outbox.OutboxType;

/**
 * 외부 API 호출만 대체하고 outbox 상태 변경은 실제 DB로 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SocialRevokeServiceIntegrationTest extends TestOnContainer {

    @Autowired
    private SocialRevokeService socialRevokeService;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private KakaoApiClient kakaoApiClient;

    @MockitoBean
    private AppleApiClient appleApiClient;

    @DisplayName("payload 역직렬화에 실패하면 외부 API를 호출하지 않고 즉시 FAILED로 전환한다")
    @Test
    void processInvalidPayloadFailsImmediately() {
        // given
        Outbox outbox = outboxRepository.save(Outbox.pending(OutboxType.SOCIAL_REVOKE, "not-json"));

        // when
        SocialRevokeResult result = socialRevokeService.process();

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        verify(kakaoApiClient, never()).unlink(anyString());
        verify(appleApiClient, never()).revoke(anyString());
        assertAll(
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.FAILED),
            () -> assertThat(found.getFailCount()).isEqualTo(1),
            () -> assertThat(found.getPayload()).isEqualTo("not-json"),
            () -> assertThat(result.failedCount()).isEqualTo(1)
        );
    }

    @DisplayName("외부 API 호출에 실패하면 실패 횟수만 증가하고 PENDING으로 남아 재시도 대상이 된다")
    @Test
    void processApiFailureStaysPending() {
        // given
        Outbox outbox = outboxService.save(OutboxType.SOCIAL_REVOKE,
            new SocialRevokePayload(1L, SocialProvider.KAKAO, "kakao-user-1", null));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoApiClient).unlink("kakao-user-1");

        // when
        SocialRevokeResult result = socialRevokeService.process();

        // then
        Outbox found = outboxRepository.getByIdOrThrow(outbox.getId());
        assertAll(
            () -> assertThat(found.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(found.getFailCount()).isEqualTo(1),
            () -> assertThat(result.failedCount()).isEqualTo(1)
        );
    }
}
