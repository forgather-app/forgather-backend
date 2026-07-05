package com.forgather.global.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtParser jwtParser;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private KakaoAuthClient kakaoAuthClient;

    @Mock
    private KakaoHostRepository kakaoHostRepository;

    @Mock
    private HostRepository hostRepository;

    @InjectMocks
    private AuthService authService;

    @DisplayName("신규 카카오 가입은 서비스 닉네임을 비우고 카카오 원본 사용자명을 저장한다")
    @Test
    void createKakaoHostWithSeparatedNames() {
        // given
        KakaoIdToken idToken = new KakaoIdToken(null, "kakao-user-id", null, null, "카카오닉네임", null, null,
            "pictureUrl");
        when(jwtParser.parseIdToken("id-token")).thenReturn(idToken);
        when(kakaoHostRepository.findByUserId("kakao-user-id")).thenReturn(Optional.empty());
        when(kakaoHostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(nullable(Long.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(nullable(Long.class))).thenReturn("refresh-token");

        // when
        authService.kakaoLoginConfirm(kakaoLoginConfirmRequest());

        ArgumentCaptor<KakaoHost> captor = ArgumentCaptor.forClass(KakaoHost.class);
        verify(kakaoHostRepository).save(captor.capture());
        KakaoHost savedKakaoHost = captor.getValue();

        // then
        assertAll(
            () -> assertThat(savedKakaoHost.getHost().getName()).isNull(),
            () -> assertThat(savedKakaoHost.getName()).isEqualTo("카카오닉네임")
        );
    }

    @DisplayName("카카오 id token에 닉네임이 없으면 로그인에 실패한다")
    @Test
    void failKakaoLoginWhenNicknameIsMissing() {
        // given
        KakaoIdToken idToken = new KakaoIdToken(null, "kakao-user-id", null, null, null, null, null, "pictureUrl");
        when(jwtParser.parseIdToken("id-token")).thenReturn(idToken);

        // when, then
        assertThatThrownBy(() -> authService.kakaoLoginConfirm(kakaoLoginConfirmRequest()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("카카오 닉네임");
    }

    private KakaoLoginConfirmRequest kakaoLoginConfirmRequest() {
        return new KakaoLoginConfirmRequest(
            "access-token",
            "bearer",
            "refresh-token",
            "id-token",
            3600L,
            "profile",
            "604800"
        );
    }
}
