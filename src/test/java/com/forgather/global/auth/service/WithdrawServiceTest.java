package com.forgather.global.auth.service;

import static com.forgather.fixture.HostFixture.createHostWithId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;

@ExtendWith(MockitoExtension.class)
class WithdrawServiceTest {

    @Mock
    private KakaoHostRepository kakaoHostRepository;

    @Mock
    private AppleHostRepository appleHostRepository;

    @Mock
    private SocialRevokeService socialRevokeService;

    @Mock
    private HostDeleteService hostDeleteService;

    private WithdrawService createService() {
        return new WithdrawService(
            kakaoHostRepository, appleHostRepository, socialRevokeService, hostDeleteService);
    }

    @DisplayName("Kakao 회원 탈퇴 시 unlink를 먼저 수행한 뒤 삭제한다")
    @Test
    void withdrawKakaoHost() {
        // given
        WithdrawService service = createService();
        Host host = createHostWithId(1L);
        when(kakaoHostRepository.findByHost(host))
            .thenReturn(Optional.of(new KakaoHost(host, "kakao-user-1")));
        when(appleHostRepository.findByHost(host)).thenReturn(Optional.empty());

        // when
        service.withdraw(host);

        // then
        InOrder order = inOrder(socialRevokeService, hostDeleteService);
        order.verify(socialRevokeService).revokeKakao("kakao-user-1");
        order.verify(hostDeleteService).delete(1L);
    }

    @DisplayName("Apple 회원 탈퇴 시 저장된 refresh token으로 revoke를 수행한다")
    @Test
    void withdrawAppleHost() {
        // given
        WithdrawService service = createService();
        Host host = createHostWithId(1L);
        when(kakaoHostRepository.findByHost(host)).thenReturn(Optional.empty());
        when(appleHostRepository.findByHost(host))
            .thenReturn(Optional.of(new AppleHost(host, "apple-user-1", "apple-refresh-token")));

        // when
        service.withdraw(host);

        // then
        verify(socialRevokeService).revokeApple("apple-user-1", "apple-refresh-token");
        verify(hostDeleteService).delete(1L);
    }

    @DisplayName("소셜 매핑이 없는 회원은 연결 해제 없이 삭제만 수행한다")
    @Test
    void withdrawHostWithoutSocialAccount() {
        // given
        WithdrawService service = createService();
        Host host = createHostWithId(1L);
        when(kakaoHostRepository.findByHost(host)).thenReturn(Optional.empty());
        when(appleHostRepository.findByHost(host)).thenReturn(Optional.empty());

        // when
        service.withdraw(host);

        // then
        verify(socialRevokeService, never()).revokeKakao(anyString());
        verify(socialRevokeService, never()).revokeApple(anyString(), any());
        verify(hostDeleteService).delete(1L);
    }
}
