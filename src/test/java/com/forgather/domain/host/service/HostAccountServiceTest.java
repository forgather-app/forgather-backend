package com.forgather.domain.host.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.fixture.HostFixture.createHostWithId;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.dto.HostResponse;
import com.forgather.domain.host.dto.OnboardingRequest;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.TermRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class HostAccountServiceTest {

    @Mock
    private HostRepository hostRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private HostTermHistoryRepository hostTermHistoryRepository;

    @Mock
    private HostProfilePhotoRepository hostProfilePhotoRepository;

    private HostAccountService hostAccountService;

    @BeforeEach
    void setUp() {
        hostAccountService = new HostAccountService(
            hostRepository,
            termRepository,
            hostTermHistoryRepository,
            hostProfilePhotoRepository
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        HostResponse result = hostAccountService.getAccount(host);

        // then
        assertThat(result.onboardingCompleted()).isTrue();
    }

    @DisplayName("닉네임 설정이 되지 않았으면 온보딩 미완료이다")
    @Test
    void onboardingNotFinishedWhenInvalidNickname() {
        // given
        Host host = createHostWithId(1L);

        // when
        HostResponse result = hostAccountService.getAccount(host);

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
        HostResponse result = hostAccountService.getAccount(host);

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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(onboardingHost(), request))
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
        when(hostRepository.getByIdWithLockOrThrow(1L)).thenReturn(host);

        // when
        hostAccountService.submitOnboarding(host, request);

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
        OnboardingRequest request = new OnboardingRequest("포개더", List.of(1L, 2L), List.of(3L));
        when(hostRepository.getByIdWithLockOrThrow(1L)).thenReturn(host);
        when(hostTermHistoryRepository.findAgreedTermTypesByHostId(1L)).thenReturn(TermType.requiredTypes());

        // when & then
        assertThatThrownBy(() -> hostAccountService.submitOnboarding(host, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("이미 온보딩이 완료된 호스트입니다. hostId: 1");
        verify(hostTermHistoryRepository, never()).saveAll(any());
    }

    /**
     * 온보딩은 호스트 행 락 조회를 가장 먼저 수행하므로, 약관 검증 실패 케이스도 락 조회 스텁이 필요하다.
     */
    private Host onboardingHost() {
        Host host = createHostWithId(1L);
        when(hostRepository.getByIdWithLockOrThrow(1L)).thenReturn(host);
        return host;
    }

    private Term termWithId(Term term, long id) {
        ReflectionTestUtils.setField(term, "id", id);
        return term;
    }
}
