package com.forgather.global.auth.service;

import static com.forgather.fixture.HostFixture.createHostWithoutEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.http.HttpStatus;

import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.AppleApiClient;
import com.forgather.global.external.social.SocialJwtParser;
import com.forgather.global.external.social.dto.AppleIdToken;
import com.forgather.global.external.social.dto.AppleTokenResponse;
import com.forgather.global.external.social.dto.KakaoIdToken;
import com.forgather.global.util.RandomCodeGenerator;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SocialJwtParser socialJwtParser;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private KakaoHostRepository kakaoHostRepository;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private AppleHostRepository appleHostRepository;

    @Mock
    private AppleApiClient appleApiClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            socialJwtParser,
            jwtTokenProvider,
            kakaoHostRepository,
            hostRepository,
            appleHostRepository,
            appleApiClient,
            new RandomCodeGenerator()
        );
    }

    @DisplayName("신규 Apple 로그인은 code 교환 응답으로 Host와 AppleHost를 생성하고 토큰을 발급한다")
    @Test
    void appleLoginConfirm_createsAppleHost() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "authorization-code",
            "raw-nonce",
            "홍길동"
        );
        when(appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("apple-refresh-token"));
        when(socialJwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
        assertThat(saved.getRefreshToken()).isEqualTo("apple-refresh-token");
        assertThat(saved.getHost().getName()).isEqualTo("홍길동");
        assertThat(saved.getHost().getEmail()).isEqualTo("apple@example.com");
        assertThat(saved.getHost().getCode()).matches("[0-9a-z]{10}");
        assertThat(saved.getHost().getPictureUrl()).isNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(hostRepository).save(saved.getHost());
    }

    @DisplayName("기존 AppleHost 로그인은 이름 없이 refresh token을 갱신하고 기존 Host로 토큰을 발급한다")
    @Test
    void appleLoginConfirm_existingAppleHostDoesNotRequireFullName() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "authorization-code",
            "raw-nonce",
            null
        );
        Host host = new Host(HostFixture.randomCode(), "기존사용자", "old@example.com");
        AppleHost appleHost = new AppleHost(host, "apple-sub", "old-apple-refresh-token");
        when(appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("new-apple-refresh-token"));
        when(socialJwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
        assertThat(appleHost.getRefreshToken()).isEqualTo("new-apple-refresh-token");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @DisplayName("신규 Apple 로그인에서 이름이 없으면 기본 이름으로 가입한다")
    @Test
    void appleLoginConfirm_fallsBackToDefaultNameWhenFullNameMissing() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "authorization-code",
            "raw-nonce",
            null
        );
        when(appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("apple-refresh-token"));
        when(socialJwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
        assertAll(
            () -> assertThat(saved.getHost().getName()).isEqualTo("Apple 사용자"),
            () -> assertThat(saved.getHost().getEmail()).isEqualTo("apple@example.com"),
            () -> assertThat(saved.getHost().getCode()).matches("[0-9a-z]{10}"),
            () -> assertThat(saved.getUserId()).isEqualTo("apple-sub"),
            () -> assertThat(saved.getRefreshToken()).isEqualTo("apple-refresh-token"),
            () -> assertThat(response.accessToken()).isEqualTo("access-token")
        );
    }

    @DisplayName("신규 Apple 로그인에서 이름이 공백뿐이면 기본 이름으로 가입한다")
    @Test
    void appleLoginConfirm_fallsBackToDefaultNameWhenFullNameBlank() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "authorization-code",
            "raw-nonce",
            "   "
        );
        when(appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("apple-refresh-token"));
        when(socialJwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
        authService.appleLoginConfirm(request);

        // then
        ArgumentCaptor<AppleHost> captor = ArgumentCaptor.forClass(AppleHost.class);
        verify(appleHostRepository).save(captor.capture());
        assertThat(captor.getValue().getHost().getName()).isEqualTo("Apple 사용자");
    }

    private AppleTokenResponse appleTokenResponse(String refreshToken) {
        return new AppleTokenResponse(
            "apple-access-token",
            "Bearer",
            3600L,
            refreshToken,
            "apple-server-id-token"
        );
    }

    @DisplayName("신규 카카오 가입은 원본 사용자명을 이름으로 저장하고 서비스 닉네임은 비운다")
    @Test
    void createKakaoHostWithSeparatedNames() {
        // given
        KakaoIdToken idToken = kakaoIdToken("카카오닉네임", "kakao@example.com");
        when(socialJwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
        when(kakaoHostRepository.findByUserId("kakao-user-id")).thenReturn(Optional.empty());
        when(hostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
            () -> assertThat(savedKakaoHost.getHost().getNickname()).isNull(),
            () -> assertThat(savedKakaoHost.getHost().getEmail()).isEqualTo("kakao@example.com")
        );
    }

    @DisplayName("호스트 코드가 이미 사용 중이면 새 코드로 다시 시도한다")
    @Test
    void regeneratesHostCodeWhenAlreadyUsed() {
        // given
        KakaoIdToken idToken = kakaoIdToken("카카오닉네임", "kakao@example.com");
        when(socialJwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
        when(kakaoHostRepository.findByUserId("kakao-user-id")).thenReturn(Optional.empty());
        when(hostRepository.existsByCode(anyString())).thenReturn(true, true, false);
        when(hostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kakaoHostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(nullable(Long.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(nullable(Long.class))).thenReturn("refresh-token");

        // when
        authService.kakaoLoginConfirm(kakaoLoginConfirmRequest());

        // then
        ArgumentCaptor<KakaoHost> captor = ArgumentCaptor.forClass(KakaoHost.class);
        verify(kakaoHostRepository).save(captor.capture());
        assertThat(captor.getValue().getHost().getCode()).matches("[0-9a-z]{10}");
        verify(hostRepository, times(3)).existsByCode(anyString());
    }

    @DisplayName("호스트 코드가 계속 사용 중이면 가입에 실패한다")
    @Test
    void failsWhenEveryHostCodeAttemptIsUsed() {
        // given
        KakaoIdToken idToken = kakaoIdToken("카카오닉네임", "kakao@example.com");
        when(socialJwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
        when(kakaoHostRepository.findByUserId("kakao-user-id")).thenReturn(Optional.empty());
        when(hostRepository.existsByCode(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.kakaoLoginConfirm(kakaoLoginConfirmRequest()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("호스트 코드를 생성하지 못했습니다")
            .satisfies(thrown -> assertThat(((BaseException)thrown).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        verify(hostRepository, never()).save(any());
    }

    @DisplayName("기존 카카오 회원이 재로그인하면 id token의 email로 Host email을 채운다")
    @Test
    void backfillEmailWhenExistingKakaoHostLogsIn() {
        // given
        Host host = createHostWithoutEmail("카카오원본이름");
        KakaoHost kakaoHost = new KakaoHost(host, "kakao-user-id");
        when(socialJwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(kakaoIdToken("카카오닉네임", "kakao@example.com"));
        when(kakaoHostRepository.findByUserId("kakao-user-id")).thenReturn(Optional.of(kakaoHost));
        when(jwtTokenProvider.generateAccessToken(nullable(Long.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(nullable(Long.class))).thenReturn("refresh-token");

        // when
        authService.kakaoLoginConfirm(kakaoLoginConfirmRequest());

        // then
        assertAll(
            () -> assertThat(host.getEmail()).isEqualTo("kakao@example.com"),
            () -> verify(hostRepository, never()).save(any()),
            () -> verify(kakaoHostRepository, never()).save(any())
        );
    }

    @DisplayName("카카오 id token에 닉네임이 없으면 로그인에 실패한다")
    @Test
    void failKakaoLoginWhenNicknameIsMissing() {
        // given
        KakaoIdToken idToken = kakaoIdToken(null, "kakao@example.com");
        when(socialJwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);

        // when, then
        assertThatThrownBy(() -> authService.kakaoLoginConfirm(kakaoLoginConfirmRequest()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("이름");
    }

    private KakaoIdToken kakaoIdToken(String nickname, String email) {
        return new KakaoIdToken(
            "https://kauth.kakao.com",
            "test-kakao-native-app-key",
            "kakao-user-id",
            1L,
            1L,
            1L,
            "nonce",
            nickname,
            email
        );
    }

    private KakaoLoginConfirmRequest kakaoLoginConfirmRequest() {
        return new KakaoLoginConfirmRequest(
            "access-token",
            "bearer",
            "refresh-token",
            "id-token",
            "raw-nonce",
            3600L,
            "profile",
            "604800"
        );
    }
}
