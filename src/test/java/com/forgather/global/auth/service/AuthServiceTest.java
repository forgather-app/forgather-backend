package com.forgather.global.auth.service;

import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.HostFixture.createHostWithId;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.client.KakaoAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
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
    private TermRepository termRepository;

    @Mock
    private HostTermHistoryRepository hostTermHistoryRepository;

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
            termRepository,
            hostTermHistoryRepository,
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

        // when, then
        assertThatThrownBy(() -> authService.appleLoginConfirm(request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("회원가입을 위해 애플 사용자 정보가 필요합니다.");
    }

    @DisplayName("신규 카카오 가입은 원본 사용자명을 이름으로 저장하고 서비스 닉네임은 비운다")
    @Test
    void createKakaoHostWithSeparatedNames() {
        // given
        KakaoIdToken idToken = new KakaoIdToken(null, "kakao-user-id", null, null, "카카오닉네임", null, null,
            "pictureUrl");
        when(jwtParser.parseKakaoIdToken("id-token")).thenReturn(idToken);
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
            () -> assertThat(savedKakaoHost.getHost().getName()).isEqualTo("카카오닉네임"),
            () -> assertThat(savedKakaoHost.getHost().getNickname()).isNull()
        );
    }

    @DisplayName("카카오 id token에 닉네임이 없으면 로그인에 실패한다")
    @Test
    void failKakaoLoginWhenNicknameIsMissing() {
        // given
        KakaoIdToken idToken = new KakaoIdToken(null, "kakao-user-id", null, null, null, null, null, "pictureUrl");
        when(jwtParser.parseKakaoIdToken("id-token")).thenReturn(idToken);

        // when, then
        assertThatThrownBy(() -> authService.kakaoLoginConfirm(kakaoLoginConfirmRequest()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("이름");
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

    @DisplayName("유효하지 않은 약관 id에 동의하면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenTermIdInvalid() {
        // given
        List<Long> agreedTermIds = List.of(1L, 2L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", agreedTermIds);
        when(termRepository.findByIdInAndDeletedAtIsNull(agreedTermIds)).thenReturn(List.of());

        // when, then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("존재하지 않거나 삭제된 약관 ID가 포함되어 있습니다.");
    }

    @DisplayName("필수 동의 약관에 동의하지 않으면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenNotAgreedRequiredTerm() {
        // given
        List<Long> agreedTermIds = List.of(1L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", agreedTermIds);
        when(termRepository.findByIdInAndDeletedAtIsNull(agreedTermIds))
            .thenReturn(List.of(createPrivacyTerm("1", "content")));

        // when, then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("필수 약관 동의가 누락되었습니다.");
    }

    @DisplayName("닉네임 설정과 필수 동의 약관에 동의하면 온보딩 완료이다")
    @Test
    void onboardingFinishedWhenSetNicknameAndAgreedRequiredTerm() {
        // given
        Host host = createHostWithId(1L);
        host.updateNickname("kjyyjk");
        when(hostTermHistoryRepository.findAgreedTermTypesByHostId(1L))
            .thenReturn(TermType.requiredTypes());

        // when
        HostResponse result = authService.getCurrentUser(host);

        // then
        assertThat(result.onboardingCompleted()).isTrue();
    }

    @DisplayName("닉네임 설정이 되지 않았으면 온보딩 미완료이다")
    @Test
    void onboardingNotFinishedWhenInvalidNickname() {
        // given
        Host host = createHostWithId(1L);

        // when
        HostResponse result = authService.getCurrentUser(host);

        // then
        assertThat(result.onboardingCompleted()).isFalse();
    }

    @DisplayName("필수 약관에 동의한 이력이 없으면 온보딩 미완료이다")
    @Test
    void onboardingNotFinishedWhenNotAgreedRequiredTerm() {
        // given
        Host host = createHostWithId(1L);
        host.updateNickname("kjyyjk");
        when(hostTermHistoryRepository.findAgreedTermTypesByHostId(1L))
            .thenReturn(Set.of());

        // when
        HostResponse result = authService.getCurrentUser(host);

        // then
        assertThat(result.onboardingCompleted()).isFalse();
    }
}
