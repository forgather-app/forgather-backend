package com.forgather.global.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forgather.global.auth.client.AppleAuthClient;
import com.forgather.global.auth.client.KakaoUnlinkClient;
import com.forgather.global.auth.client.SocialProvider;
import com.forgather.global.auth.model.SocialRevokeFailLog;
import com.forgather.global.auth.repository.SocialRevokeFailLogRepository;
import com.forgather.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class SocialRevokeServiceTest {

    @Mock
    private KakaoUnlinkClient kakaoUnlinkClient;

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private SocialRevokeFailLogRepository socialRevokeFailLogRepository;

    private SocialRevokeService createService() {
        return new SocialRevokeService(kakaoUnlinkClient, appleAuthClient, socialRevokeFailLogRepository);
    }

    @DisplayName("Kakao unlink에 성공하면 실패 로그를 남기지 않는다")
    @Test
    void revokeKakaoSuccess() {
        // given
        SocialRevokeService service = createService();

        // when
        service.revokeKakao("kakao-user-1");

        // then
        verify(kakaoUnlinkClient).unlink("kakao-user-1");
        verify(socialRevokeFailLogRepository, never()).save(any());
    }

    @DisplayName("Kakao unlink에 실패하면 예외를 전파하지 않고 실패 로그를 저장한다")
    @Test
    void revokeKakaoFailure() {
        // given
        SocialRevokeService service = createService();
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoUnlinkClient).unlink("kakao-user-1");

        // when
        service.revokeKakao("kakao-user-1");

        // then
        ArgumentCaptor<SocialRevokeFailLog> captor = ArgumentCaptor.forClass(SocialRevokeFailLog.class);
        verify(socialRevokeFailLogRepository).save(captor.capture());
        assertAll(
            () -> assertThat(captor.getValue().getProvider()).isEqualTo(SocialProvider.KAKAO),
            () -> assertThat(captor.getValue().getSocialUserId()).isEqualTo("kakao-user-1")
        );
    }

    @DisplayName("Apple revoke에 실패하면 재시도를 위해 refresh token을 실패 로그에 보관한다")
    @Test
    void revokeAppleFailure() {
        // given
        SocialRevokeService service = createService();
        doThrow(new BaseException("Apple token revoke에 실패했습니다."))
            .when(appleAuthClient).revoke("apple-refresh-token");

        // when
        service.revokeApple("apple-user-1", "apple-refresh-token");

        // then
        ArgumentCaptor<SocialRevokeFailLog> captor = ArgumentCaptor.forClass(SocialRevokeFailLog.class);
        verify(socialRevokeFailLogRepository).save(captor.capture());
        assertAll(
            () -> assertThat(captor.getValue().getProvider()).isEqualTo(SocialProvider.APPLE),
            () -> assertThat(captor.getValue().getSocialUserId()).isEqualTo("apple-user-1"),
            () -> assertThat(captor.getValue().getAppleRefreshToken()).isEqualTo("apple-refresh-token")
        );
    }

    @DisplayName("재시도에 성공한 실패 로그는 완료 처리하고, 실패한 로그는 실패 횟수를 증가시킨다")
    @Test
    void retryFailedRevokes() {
        // given
        SocialRevokeService service = createService();
        SocialRevokeFailLog kakaoLog = SocialRevokeFailLog.kakao("kakao-user-1");
        SocialRevokeFailLog appleLog = SocialRevokeFailLog.apple("apple-user-1", "apple-refresh-token");
        when(socialRevokeFailLogRepository.findAllByCompletedAtIsNull())
            .thenReturn(List.of(kakaoLog, appleLog));
        doThrow(new BaseException("Kakao unlink에 실패했습니다."))
            .when(kakaoUnlinkClient).unlink("kakao-user-1");

        // when
        service.retryFailedRevokes();

        // then
        assertAll(
            () -> assertThat(kakaoLog.getCompletedAt()).isNull(),
            () -> assertThat(kakaoLog.getFailCount()).isEqualTo(2),
            () -> assertThat(appleLog.getCompletedAt()).isNotNull(),
            () -> assertThat(appleLog.getFailCount()).isEqualTo(1)
        );
    }
}
