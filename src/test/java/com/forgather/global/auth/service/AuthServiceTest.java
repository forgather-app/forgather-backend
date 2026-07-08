package com.forgather.global.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.AppleHostRepository;
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

    @Mock
    private AppleHostRepository appleHostRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            jwtParser,
            jwtTokenProvider,
            kakaoAuthClient,
            kakaoHostRepository,
            hostRepository,
            appleHostRepository,
            new ObjectMapper()
        );
    }

    @DisplayName("신규 Apple 로그인은 user JSON으로 Host와 AppleHost를 생성하고 토큰을 발급한다")
    @Test
    void appleLoginConfirm_createsAppleHost() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "{\"name\":{\"firstName\":\"길동\",\"lastName\":\"홍\"},\"email\":\"ignored@example.com\"}",
            "raw-nonce"
        );
        when(jwtParser.parseAppleIdToken("id-token", "raw-nonce"))
            .thenReturn(new AppleIdToken(
                "https://appleid.apple.com",
                "test-apple-audience",
                "apple-sub",
                "apple@example.com",
                true,
                1L,
                1L,
                "nonce"
            ));
        when(appleHostRepository.findByUserId("apple-sub")).thenReturn(Optional.empty());
        when(appleHostRepository.save(any(AppleHost.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(nullable(Long.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(nullable(Long.class))).thenReturn("refresh-token");

        // when
        LoginResponse response = authService.appleLoginConfirm(request);

        // then
        ArgumentCaptor<AppleHost> captor = ArgumentCaptor.forClass(AppleHost.class);
        verify(appleHostRepository).save(captor.capture());
        AppleHost saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("apple-sub");
        assertThat(saved.getHost().getName()).isEqualTo("홍길동");
        assertThat(saved.getHost().getEmail()).isEqualTo("apple@example.com");
        assertThat(saved.getHost().getPictureUrl()).isNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(hostRepository).save(saved.getHost());
    }

    @DisplayName("기존 AppleHost 로그인은 user 없이 기존 Host로 토큰을 발급한다")
    @Test
    void appleLoginConfirm_existingAppleHostDoesNotRequireUser() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest("id-token", null, "raw-nonce");
        Host host = new Host("기존사용자", null, "old@example.com");
        AppleHost appleHost = new AppleHost(host, "apple-sub");
        when(jwtParser.parseAppleIdToken("id-token", "raw-nonce"))
            .thenReturn(new AppleIdToken(
                "https://appleid.apple.com",
                "test-apple-audience",
                "apple-sub",
                "new@example.com",
                "true",
                1L,
                1L,
                "nonce"
            ));
        when(appleHostRepository.findByUserId("apple-sub")).thenReturn(Optional.of(appleHost));
        when(jwtTokenProvider.generateAccessToken(nullable(Long.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(nullable(Long.class))).thenReturn("refresh-token");

        // when
        LoginResponse response = authService.appleLoginConfirm(request);

        // then
        verify(appleHostRepository, never()).save(any());
        assertThat(host.getEmail()).isEqualTo("old@example.com");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @DisplayName("신규 Apple 로그인에서 user가 없으면 실패한다")
    @Test
    void appleLoginConfirm_newAppleHostRequiresUser() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest("id-token", null, "raw-nonce");
        when(jwtParser.parseAppleIdToken("id-token", "raw-nonce"))
            .thenReturn(new AppleIdToken(
                "https://appleid.apple.com",
                "test-apple-audience",
                "apple-sub",
                "apple@example.com",
                true,
                1L,
                1L,
                "nonce"
            ));
        when(appleHostRepository.findByUserId("apple-sub")).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> authService.appleLoginConfirm(request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("회원가입을 위해 애플 사용자 정보가 필요합니다.");
    }
}
