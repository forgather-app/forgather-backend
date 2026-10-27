package com.forgather.domain.host.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.host.service.SocialRevokePayload;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.client.AppleApiClient;
import com.forgather.global.external.social.client.KakaoApiClient;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxTaskResult;
import com.forgather.global.outbox.OutboxType;

@ExtendWith(MockitoExtension.class)
class SocialRevokeHandlerTest {

    @Mock private KakaoApiClient kakaoApiClient;
    @Mock private AppleApiClient appleApiClient;
    @Mock private KakaoHostRepository kakaoHostRepository;
    @Mock private AppleHostRepository appleHostRepository;
    @Mock private KakaoHost kakaoHost;
    @Mock private AppleHost appleHost;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocialRevokeHandler handler() {
        return new SocialRevokeHandler(kakaoApiClient, appleApiClient, kakaoHostRepository, appleHostRepository, objectMapper);
    }

    private Outbox task(SocialProvider provider) throws JsonProcessingException {
        return Outbox.pending(OutboxType.SOCIAL_REVOKE,
            objectMapper.writeValueAsString(new SocialRevokePayload(1L, provider, "user-1", "refresh-token")));
    }

    @DisplayName("소셜 연결 해제 타입이면 처리할 수 있다고 응답한다")
    @Test
    void supportsSocialRevoke() {
        // given
        SocialRevokeHandler handler = handler();

        // when & then
        assertThat(handler.supports(OutboxType.SOCIAL_REVOKE)).isTrue();
    }

    @DisplayName("Kakao 연결 해제에 성공하면 완료 결과를 반환한다")
    @Test
    void unlinksKakao() throws JsonProcessingException {
        // given
        Outbox task = task(SocialProvider.KAKAO);

        // when
        OutboxTaskResult result = handler().process(task);

        // then
        assertThat(result).isEqualTo(OutboxTaskResult.COMPLETED);
        verify(kakaoApiClient).unlink("user-1");
        verifyNoInteractions(appleApiClient);
    }

    @DisplayName("Apple 연결 해제에 성공하면 완료 결과를 반환한다")
    @Test
    void revokesApple() throws JsonProcessingException {
        // given
        Outbox task = task(SocialProvider.APPLE);

        // when
        OutboxTaskResult result = handler().process(task);

        // then
        assertThat(result).isEqualTo(OutboxTaskResult.COMPLETED);
        verify(appleApiClient).revoke("refresh-token");
        verifyNoInteractions(kakaoApiClient);
    }

    @DisplayName("같은 Kakao 계정으로 재가입했다면 외부 호출 없이 취소 결과를 반환한다")
    @Test
    void cancelsReRegisteredKakao() throws JsonProcessingException {
        // given
        Outbox task = task(SocialProvider.KAKAO);
        when(kakaoHostRepository.findByUserId("user-1")).thenReturn(Optional.of(kakaoHost));

        // when
        OutboxTaskResult result = handler().process(task);

        // then
        assertThat(result).isEqualTo(OutboxTaskResult.CANCELED);
        verifyNoInteractions(kakaoApiClient, appleApiClient);
    }

    @DisplayName("같은 Apple 계정으로 재가입했다면 외부 호출 없이 취소 결과를 반환한다")
    @Test
    void cancelsReRegisteredApple() throws JsonProcessingException {
        // given
        Outbox task = task(SocialProvider.APPLE);
        when(appleHostRepository.findByUserId("user-1")).thenReturn(Optional.of(appleHost));

        // when
        OutboxTaskResult result = handler().process(task);

        // then
        assertThat(result).isEqualTo(OutboxTaskResult.CANCELED);
        verifyNoInteractions(kakaoApiClient, appleApiClient);
    }

    @DisplayName("payload를 해석할 수 없으면 외부 호출 없이 영구 실패 결과를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}"})
    void failsInvalidPayload(String payload) {
        // given
        Outbox task = Outbox.pending(OutboxType.SOCIAL_REVOKE, payload);

        // when
        OutboxTaskResult result = handler().process(task);

        // then
        assertThat(result).isEqualTo(OutboxTaskResult.FAILED);
        verifyNoInteractions(kakaoApiClient, appleApiClient);
    }

    @DisplayName("외부 호출 실패는 공통 실행 흐름에서 재시도할 수 있도록 예외를 전파한다")
    @Test
    void propagatesApiFailure() throws JsonProcessingException {
        // given
        Outbox task = task(SocialProvider.KAKAO);
        doThrow(new BaseException("연결 해제 실패")).when(kakaoApiClient).unlink("user-1");

        // when & then
        assertThatThrownBy(() -> handler().process(task))
            .isInstanceOf(BaseException.class);
    }
}
