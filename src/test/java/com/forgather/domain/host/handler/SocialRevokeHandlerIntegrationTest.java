package com.forgather.domain.host.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.host.service.SocialRevokePayload;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxProcessResult;
import com.forgather.global.outbox.OutboxRepository;
import com.forgather.global.outbox.OutboxScheduler;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxStatus;
import com.forgather.global.outbox.OutboxType;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SocialRevokeHandlerIntegrationTest extends TestOnContainer {

    @Autowired private OutboxService outboxService;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private HostRepository hostRepository;
    @Autowired private KakaoHostRepository kakaoHostRepository;
    @Autowired private AppleHostRepository appleHostRepository;
    @MockitoBean private KakaoApiClient kakaoApiClient;
    @MockitoBean private AppleApiClient appleApiClient;
    @MockitoBean private OutboxScheduler outboxScheduler;

    private Outbox save(SocialProvider provider, String userId) {
        return outboxService.save(OutboxType.SOCIAL_REVOKE,
            new SocialRevokePayload(1L, provider, userId, "refresh-token"));
    }

    @DisplayName("Kakao와 Apple 연결 해제 성공 시 공통 흐름이 완료 처리하고 payload를 비운다")
    @Test
    void completesSocialTasks() {
        // given
        Outbox kakao = save(SocialProvider.KAKAO, "kakao-user-1");
        Outbox apple = save(SocialProvider.APPLE, "apple-user-1");

        // when
        OutboxProcessResult result = outboxService.process();

        // then
        verify(kakaoApiClient).unlink("kakao-user-1");
        verify(appleApiClient).revoke("refresh-token");
        assertThat(result.succeededCount()).isEqualTo(2);
        for (Outbox task : new Outbox[] {kakao, apple}) {
            Outbox stored = outboxRepository.findById(task.getId()).orElseThrow();
            assertAll(
                () -> assertThat(stored.getStatus()).isEqualTo(OutboxStatus.COMPLETED),
                () -> assertThat(stored.getPayload()).isNull(),
                () -> assertThat(stored.getLeaseExpiresAt()).isNull()
            );
        }
    }

    @DisplayName("payload 역직렬화 실패 시 외부 호출 없이 즉시 FAILED로 전환한다")
    @Test
    void failsInvalidPayloadImmediately() {
        // given
        Outbox task = outboxRepository.save(Outbox.pending(OutboxType.SOCIAL_REVOKE, "not-json"));

        // when
        OutboxProcessResult result = outboxService.process();

        // then
        Outbox stored = outboxRepository.findById(task.getId()).orElseThrow();
        verify(kakaoApiClient, never()).unlink(anyString());
        verify(appleApiClient, never()).revoke(anyString());
        assertAll(
            () -> assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED),
            () -> assertThat(stored.getFailCount()).isEqualTo(1),
            () -> assertThat(stored.getPayload()).isEqualTo("not-json"),
            () -> assertThat(result.failedCount()).isEqualTo(1)
        );
    }

    @DisplayName("외부 API 호출 실패 시 PENDING으로 돌리고 다섯 번째 실패 후 재시도하지 않는다")
    @Test
    void stopsAfterFiveApiFailures() {
        // given
        Outbox task = save(SocialProvider.KAKAO, "kakao-user-1");
        doThrow(new BaseException("연결 해제 실패")).when(kakaoApiClient).unlink("kakao-user-1");

        // when & then
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(outboxService.process().failedCount()).isEqualTo(1);
            Outbox stored = outboxRepository.findById(task.getId()).orElseThrow();
            int expectedFailCount = attempt;
            assertAll(
                () -> assertThat(stored.getStatus()).isEqualTo(expectedFailCount < 5 ? OutboxStatus.PENDING : OutboxStatus.FAILED),
                () -> assertThat(stored.getFailCount()).isEqualTo(expectedFailCount),
                () -> assertThat(stored.getPayload()).isEqualTo(task.getPayload())
            );
        }
        assertThat(outboxService.process().isEmpty()).isTrue();
        verify(kakaoApiClient, times(5)).unlink("kakao-user-1");
    }

    @DisplayName("Kakao 재가입 시 선점된 작업을 취소하고 외부 연결을 해제하지 않는다")
    @Test
    void cancelsReRegisteredKakao() {
        // given
        Outbox task = save(SocialProvider.KAKAO, "kakao-user-1");
        Host host = hostRepository.save(HostFixture.createHost());
        kakaoHostRepository.save(new KakaoHost(host, "kakao-user-1"));

        // when
        OutboxProcessResult result = outboxService.process();

        // then
        verify(kakaoApiClient, never()).unlink(anyString());
        Outbox stored = outboxRepository.findById(task.getId()).orElseThrow();
        assertAll(
            () -> assertThat(result.canceledCount()).isEqualTo(1),
            () -> assertThat(stored.getStatus()).isEqualTo(OutboxStatus.CANCELED),
            () -> assertThat(stored.getPayload()).isNull(),
            () -> assertThat(stored.getFailCount()).isZero()
        );
    }

    @DisplayName("Apple 재가입 시 선점된 작업을 취소하고 외부 연결을 해제하지 않는다")
    @Test
    void cancelsReRegisteredApple() {
        // given
        Outbox task = save(SocialProvider.APPLE, "apple-user-1");
        Host host = hostRepository.save(HostFixture.createHost());
        appleHostRepository.save(new AppleHost(host, "apple-user-1", "new-refresh-token"));

        // when
        OutboxProcessResult result = outboxService.process();

        // then
        verify(appleApiClient, never()).revoke(anyString());
        Outbox stored = outboxRepository.findById(task.getId()).orElseThrow();
        assertAll(
            () -> assertThat(result.canceledCount()).isEqualTo(1),
            () -> assertThat(stored.getStatus()).isEqualTo(OutboxStatus.CANCELED),
            () -> assertThat(stored.getPayload()).isNull(),
            () -> assertThat(stored.getFailCount()).isZero()
        );
    }

    @DisplayName("payload 오류와 외부 API 실패가 있어도 뒤의 작업을 완료한다")
    @Test
    void continuesAfterFailures() {
        // given
        outboxRepository.save(Outbox.pending(OutboxType.SOCIAL_REVOKE, "not-json"));
        Outbox failing = save(SocialProvider.KAKAO, "kakao-user-1");
        Outbox succeeding = save(SocialProvider.APPLE, "apple-user-1");
        doThrow(new BaseException("연결 해제 실패")).when(kakaoApiClient).unlink("kakao-user-1");

        // when
        OutboxProcessResult result = outboxService.process();

        // then
        assertAll(
            () -> assertThat(result.failedCount()).isEqualTo(2),
            () -> assertThat(result.succeededCount()).isEqualTo(1),
            () -> assertThat(outboxRepository.findById(failing.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(outboxRepository.findById(succeeding.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.COMPLETED)
        );
        verify(appleApiClient).revoke("refresh-token");
    }
}
