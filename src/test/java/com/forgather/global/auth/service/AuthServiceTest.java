package com.forgather.global.auth.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.HostFixture.createHostWithId;
import static com.forgather.fixture.HostFixture.createHostWithoutEmail;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.auth.client.AppleAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.AppleLoginConfirmRequest;
import com.forgather.global.auth.dto.AppleTokenResponse;
import com.forgather.global.auth.dto.HostResponse;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.auth.dto.KakaoLoginConfirmRequest;
import com.forgather.global.auth.dto.LoginResponse;
import com.forgather.global.auth.dto.OnboardingRequest;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.AppleHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.KakaoHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.util.JwtParser;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.util.RandomCodeGenerator;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtParser jwtParser;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

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

    @Mock
    private AppleAuthClient appleAuthClient;

    @Mock
    private HostProfilePhotoRepository hostProfilePhotoRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            jwtParser,
            jwtTokenProvider,
            kakaoHostRepository,
            hostRepository,
            termRepository,
            hostTermHistoryRepository,
            appleHostRepository,
            appleAuthClient,
            hostProfilePhotoRepository,
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
        when(appleAuthClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("apple-refresh-token"));
        when(jwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
        when(appleAuthClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("new-apple-refresh-token"));
        when(jwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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

    @DisplayName("신규 Apple 로그인에서 이름이 없으면 실패한다")
    @Test
    void appleLoginConfirm_newAppleHostRequiresFullName() {
        // given
        AppleLoginConfirmRequest request = new AppleLoginConfirmRequest(
            "id-token",
            "authorization-code",
            "raw-nonce",
            null
        );
        when(appleAuthClient.exchangeAuthorizationCode("authorization-code"))
            .thenReturn(appleTokenResponse("apple-refresh-token"));
        when(jwtParser.parseAppleIdToken("apple-server-id-token", "raw-nonce"))
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
            .hasMessageContaining("이름");
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
        when(jwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
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
        when(jwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
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
        when(jwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);
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
        when(jwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(kakaoIdToken("카카오닉네임", "kakao@example.com"));
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
        when(jwtParser.parseKakaoIdToken("id-token", "raw-nonce")).thenReturn(idToken);

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

    @DisplayName("유효하지 않은 약관 id에 동의하면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenTermIdInvalid() {
        // given
        List<Long> agreedTermIds = List.of(1L, 2L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", agreedTermIds, List.of());
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
        OnboardingRequest request = new OnboardingRequest("kjyyjk", agreedTermIds, List.of());
        when(termRepository.findByIdInAndDeletedAtIsNull(agreedTermIds))
            .thenReturn(List.of(createPrivacyTerm("1.0.0", "content")));

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

    @DisplayName("rejectedTermIds가 null이면 빈 목록으로 간주한다")
    @Test
    void normalizeNullRejectedTermIds() {
        // given
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(1L), null);

        // when & then
        assertThat(request.rejectedTermIds()).isEmpty();
    }

    @DisplayName("필수 약관을 거절하면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenRequiredTermRejected() {
        // given
        Term serviceTerm = createServiceTerm("1.0.0", "service");
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(), List.of(1L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(serviceTerm));

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("필수 약관은 거절할 수 없습니다.");
    }

    @DisplayName("같은 타입의 약관을 동의와 거절에 함께 보내면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenTypeDecisionIsContradictory() {
        // given
        Term serviceTerm = createServiceTerm("1.0.0", "service");
        Term privacyTerm = createPrivacyTerm("1.0.0", "privacy");
        Term marketingTerm = createMarketingTerm("1.0.0", "marketing");
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(1L, 2L, 3L), List.of(3L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(serviceTerm, privacyTerm, marketingTerm));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(3L))).thenReturn(List.of(marketingTerm));

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("같은 타입의 약관을 동의와 거절에 함께 보낼 수 없습니다.");
    }

    @DisplayName("선택 약관에 대한 결정이 누락되면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenOptionalTermDecisionIsMissing() {
        // given
        Term serviceTerm = termWithId(createServiceTerm("1.0.0", "service"), 1L);
        Term privacyTerm = termWithId(createPrivacyTerm("1.0.0", "privacy"), 2L);
        Term marketingTerm = termWithId(createMarketingTerm("1.0.0", "marketing"), 3L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(1L, 2L), null);
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .thenReturn(List.of(serviceTerm, privacyTerm));
        when(termRepository.findLatestTerms()).thenReturn(List.of(serviceTerm, privacyTerm, marketingTerm));

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("타입별 최신 약관 전체에 대한 동의 또는 거절이 필요합니다.");
    }

    @DisplayName("최신 버전이 아닌 구버전 약관 ID를 제출하면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenOutdatedTermIdSubmitted() {
        // given
        Term oldServiceTerm = termWithId(createServiceTerm("0.9.0", "old service"), 1L);
        Term latestServiceTerm = termWithId(createServiceTerm("1.0.0", "latest service"), 4L);
        Term privacyTerm = termWithId(createPrivacyTerm("1.0.0", "privacy"), 2L);
        Term marketingTerm = termWithId(createMarketingTerm("1.0.0", "marketing"), 3L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(1L, 2L), List.of(3L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .thenReturn(List.of(oldServiceTerm, privacyTerm));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(3L))).thenReturn(List.of(marketingTerm));
        when(termRepository.findLatestTerms()).thenReturn(List.of(latestServiceTerm, privacyTerm, marketingTerm));

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("타입별 최신 약관 전체에 대한 동의 또는 거절이 필요합니다.");
    }

    @DisplayName("같은 타입의 구버전과 최신 버전 약관을 함께 제출하면 온보딩에 실패한다")
    @Test
    void failOnboardingWhenBothOutdatedAndLatestTermSubmitted() {
        // given
        Term oldServiceTerm = termWithId(createServiceTerm("0.9.0", "old service"), 1L);
        Term latestServiceTerm = termWithId(createServiceTerm("1.0.0", "latest service"), 4L);
        Term privacyTerm = termWithId(createPrivacyTerm("1.0.0", "privacy"), 2L);
        Term marketingTerm = termWithId(createMarketingTerm("1.0.0", "marketing"), 3L);
        OnboardingRequest request = new OnboardingRequest("kjyyjk", List.of(1L, 2L, 4L), List.of(3L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L, 4L)))
            .thenReturn(List.of(oldServiceTerm, privacyTerm, latestServiceTerm));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(3L))).thenReturn(List.of(marketingTerm));
        when(termRepository.findLatestTerms()).thenReturn(List.of(latestServiceTerm, privacyTerm, marketingTerm));

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(createHost(), request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("타입별 최신 약관 전체에 대한 동의 또는 거절이 필요합니다.");
    }

    @DisplayName("최초 온보딩에서 선택 약관을 거절하면 거절 이력을 저장한다")
    @Test
    void saveRejectHistoryOnFirstOnboarding() {
        // given
        Host host = createHostWithId(1L);
        Term serviceTerm = termWithId(createServiceTerm("1.0.0", "service"), 1L);
        Term privacyTerm = termWithId(createPrivacyTerm("1.0.0", "privacy"), 2L);
        Term marketingTerm = termWithId(createMarketingTerm("1.0.0", "marketing"), 3L);
        OnboardingRequest request = new OnboardingRequest("포개더", List.of(1L, 2L), List.of(3L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .thenReturn(List.of(serviceTerm, privacyTerm));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(3L))).thenReturn(List.of(marketingTerm));
        when(termRepository.findLatestTerms()).thenReturn(List.of(serviceTerm, privacyTerm, marketingTerm));
        when(hostRepository.getByIdOrThrow(1L)).thenReturn(host);

        // when
        authService.submitOnboarding(host, request);

        // then
        ArgumentCaptor<List<HostTermHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(hostTermHistoryRepository).saveAll(captor.capture());
        assertAll(
            () -> assertThat(captor.getValue()).hasSize(3),
            () -> assertThat(captor.getValue()).extracting(HostTermHistory::getAction)
                .containsExactly(AGREE, AGREE, REJECT),
            () -> assertThat(captor.getValue().get(2).getTerm()).isEqualTo(marketingTerm)
        );
    }

    @DisplayName("이미 온보딩이 완료된 호스트가 재온보딩하면 실패한다")
    @Test
    void failOnboardingWhenAlreadyCompleted() {
        // given
        Host host = createHostWithId(1L);
        host.updateNickname("포개더");
        Term serviceTerm = termWithId(createServiceTerm("1.0.0", "service"), 1L);
        Term privacyTerm = termWithId(createPrivacyTerm("1.0.0", "privacy"), 2L);
        Term marketingTerm = termWithId(createMarketingTerm("1.0.0", "marketing"), 3L);
        OnboardingRequest request = new OnboardingRequest("포개더", List.of(1L, 2L), List.of(3L));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .thenReturn(List.of(serviceTerm, privacyTerm));
        when(termRepository.findByIdInAndDeletedAtIsNull(List.of(3L))).thenReturn(List.of(marketingTerm));
        when(termRepository.findLatestTerms()).thenReturn(List.of(serviceTerm, privacyTerm, marketingTerm));
        when(hostRepository.getByIdOrThrow(1L)).thenReturn(host);
        when(hostTermHistoryRepository.findAgreedTermTypesByHostId(1L)).thenReturn(TermType.requiredTypes());

        // when & then
        assertThatThrownBy(() -> authService.submitOnboarding(host, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("이미 온보딩이 완료된 호스트입니다. hostId: 1");
        verify(hostTermHistoryRepository, never()).saveAll(any());
    }

    private Term termWithId(Term term, long id) {
        ReflectionTestUtils.setField(term, "id", id);
        return term;
    }
}
