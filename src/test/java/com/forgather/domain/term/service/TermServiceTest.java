package com.forgather.domain.term.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.term.dto.TermAgreementResponse;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.NotFoundException;

@Import(TermService.class)
@DataJpaTest
class TermServiceTest {

    @Autowired
    private TermService termService;

    @Autowired
    private TermJpaRepository termJpaRepository;

    @Autowired
    private HostTermHistoryRepository hostTermHistoryRepository;

    @Autowired
    private HostRepository hostRepository;

    private Host host;

    @BeforeEach
    void setUp() {
        host = hostRepository.save(HostFixture.createHost());
    }

    @DisplayName("동의 이력이 없으면 전 항목이 미동의이고 필수 약관만 재동의가 필요하다")
    @Test
    void getMyTermAgreementsWithoutHistory() {
        // given
        termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result).hasSize(3),
            () -> assertThat(result).extracting(TermAgreementResponse::type)
                .containsExactly("SERVICE", "PRIVACY", "MARKETING"),
            () -> assertThat(result).extracting(TermAgreementResponse::isAgreed)
                .containsExactly(false, false, false),
            () -> assertThat(result).extracting(TermAgreementResponse::agreedAt)
                .containsExactly(null, null, null),
            () -> assertThat(result).extracting(TermAgreementResponse::isReagreementRequired)
                .containsExactly(true, true, false)
        );
    }

    @DisplayName("경미한 개정으로 최소 동의 버전이 유지되면 구버전 동의도 유효하고 약관 정보는 최신이다")
    @Test
    void keepAgreementWhenMinorRevision() {
        // given
        Term oldTerm = termJpaRepository.save(createServiceTerm("1.0.0", "old service"));
        termJpaRepository.save(createServiceTerm("1.1.0", "1.0.0", "latest service"));
        HostTermHistory history = new HostTermHistory(host, oldTerm, AGREE);
        hostTermHistoryRepository.saveAll(List.of(history));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result).hasSize(1),
            () -> assertThat(result.get(0).version()).isEqualTo("1.1.0"),
            () -> assertThat(result.get(0).content()).isEqualTo("latest service"),
            () -> assertThat(result.get(0).isAgreed()).isTrue(),
            () -> assertThat(result.get(0).agreedAt()).isEqualTo(history.getCreatedAt()),
            () -> assertThat(result.get(0).isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("실질 개정으로 최소 동의 버전이 상향되면 구버전 동의는 무효가 되고 재동의가 필요하다")
    @Test
    void requireReagreementWhenMajorRevision() {
        // given
        Term oldTerm = termJpaRepository.save(createServiceTerm("1.0.0", "old service"));
        termJpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "latest service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, oldTerm, AGREE)));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result.get(0).isAgreed()).isFalse(),
            () -> assertThat(result.get(0).agreedAt()).isNull(),
            () -> assertThat(result.get(0).isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("선택 약관을 거절했으면 재동의를 요구하지 않는다")
    @Test
    void doNotRequireReagreementWhenOptionalTermRejected() {
        // given
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, REJECT)));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result.get(0).isAgreed()).isFalse(),
            () -> assertThat(result.get(0).isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("선택 약관 동의가 개정으로 무효화되면 재동의가 필요하다")
    @Test
    void requireReagreementWhenOptionalAgreementInvalidated() {
        // given
        Term oldMarketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "old marketing"));
        termJpaRepository.save(createMarketingTerm("2.0.0", "2.0.0", "latest marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, oldMarketingTerm, AGREE)));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result.get(0).isAgreed()).isFalse(),
            () -> assertThat(result.get(0).isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("동의 후 철회하고 다시 동의하면 마지막 이력을 기준으로 판정한다")
    @Test
    void judgeByLastHistoryWhenReAgreed() {
        // given
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, serviceTerm, AGREE)));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, serviceTerm, WITHDRAW)));
        HostTermHistory reAgreed = new HostTermHistory(host, serviceTerm, AGREE);
        hostTermHistoryRepository.saveAll(List.of(reAgreed));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertAll(
            () -> assertThat(result.get(0).isAgreed()).isTrue(),
            () -> assertThat(result.get(0).agreedAt()).isEqualTo(reAgreed.getCreatedAt()),
            () -> assertThat(result.get(0).isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("다른 호스트의 동의 이력은 내 동의 현황에 반영되지 않는다")
    @Test
    void ignoreOtherHostHistory() {
        // given
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(otherHost, serviceTerm, AGREE)));

        // when
        List<TermAgreementResponse> result = termService.getMyTermAgreements(host);

        // then
        assertThat(result.get(0).isAgreed()).isFalse();
    }

    @DisplayName("미동의 선택 약관에 동의하면 동의 상태가 되고 동의 일시가 기록된다")
    @Test
    void agreeOptionalTerm() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));

        // when
        TermAgreementResponse result = termService.agreeTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.id()).isEqualTo(marketingTerm.getId()),
            () -> assertThat(result.type()).isEqualTo("MARKETING"),
            () -> assertThat(result.isAgreed()).isTrue(),
            () -> assertThat(result.agreedAt()).isNotNull(),
            () -> assertThat(result.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("개정으로 무효화된 동의는 최신 약관에 재동의하면 다시 동의 상태가 된다")
    @Test
    void reagreeInvalidatedTerm() {
        // given
        completeOnboarding();
        Term oldServiceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "old service"));
        Term latestServiceTerm = termJpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "latest service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, oldServiceTerm, AGREE)));

        // when
        TermAgreementResponse result = termService.agreeTerm(host, latestServiceTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isTrue(),
            () -> assertThat(result.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("이미 동의한 약관에 다시 동의해도 이력을 쌓지 않고 성공한다")
    @Test
    void agreeTermIsIdempotent() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        HostTermHistory agreed = new HostTermHistory(host, marketingTerm, AGREE);
        hostTermHistoryRepository.saveAll(List.of(agreed));

        // when
        TermAgreementResponse result = termService.agreeTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isTrue(),
            () -> assertThat(result.agreedAt()).isEqualTo(agreed.getCreatedAt()),
            () -> assertThat(getLastHistoryId(marketingTerm)).isEqualTo(agreed.getId())
        );
    }

    @DisplayName("최신이 아닌 약관에 동의하면 예외를 던진다")
    @Test
    void agreeOutdatedTerm() {
        // given
        Term oldServiceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "old service"));
        termJpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "latest service"));

        // when & then
        assertThatThrownBy(() -> termService.agreeTerm(host, oldServiceTerm.getId()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("최신 약관");
    }

    @DisplayName("존재하지 않는 약관에 동의하면 예외를 던진다")
    @Test
    void agreeNotFoundTerm() {
        // when & then
        assertThatThrownBy(() -> termService.agreeTerm(host, 999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않거나 삭제된 약관입니다.");
    }

    @DisplayName("삭제된 약관에 동의하면 예외를 던진다")
    @Test
    void agreeDeletedTerm() {
        // given
        Term deletedTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        deletedTerm.delete();
        termJpaRepository.save(deletedTerm);

        // when & then
        assertThatThrownBy(() -> termService.agreeTerm(host, deletedTerm.getId()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않거나 삭제된 약관입니다.");
    }

    @DisplayName("동의한 선택 약관을 철회하면 미동의 상태가 되고 재동의를 요구하지 않는다")
    @Test
    void withdrawOptionalTerm() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));

        // when
        TermAgreementResponse result = termService.withdrawTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isFalse(),
            () -> assertThat(result.agreedAt()).isNull(),
            () -> assertThat(result.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("동의하지 않은 선택 약관을 철회해도 이력을 쌓지 않고 성공한다")
    @Test
    void withdrawTermIsIdempotent() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));

        // when
        TermAgreementResponse result = termService.withdrawTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isFalse(),
            () -> assertThat(getLastHistoryId(marketingTerm)).isNull()
        );
    }

    @DisplayName("필수 약관은 철회할 수 없다")
    @Test
    void withdrawRequiredTerm() {
        // given
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, serviceTerm, AGREE)));

        // when & then
        assertThatThrownBy(() -> termService.withdrawTerm(host, serviceTerm.getId()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("필수 약관은 철회할 수 없습니다.");
    }

    @DisplayName("최신이 아닌 약관을 철회하면 예외를 던진다")
    @Test
    void withdrawOutdatedTerm() {
        // given
        Term oldMarketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "old marketing"));
        termJpaRepository.save(createMarketingTerm("2.0.0", "2.0.0", "latest marketing"));

        // when & then
        assertThatThrownBy(() -> termService.withdrawTerm(host, oldMarketingTerm.getId()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("최신 약관");
    }

    @DisplayName("존재하지 않는 약관을 철회하면 예외를 던진다")
    @Test
    void withdrawNotFoundTerm() {
        // when & then
        assertThatThrownBy(() -> termService.withdrawTerm(host, 999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않거나 삭제된 약관입니다.");
    }

    @DisplayName("철회한 선택 약관에 다시 동의하면 동의 상태로 돌아간다")
    @Test
    void agreeAfterWithdraw() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));
        termService.withdrawTerm(host, marketingTerm.getId());
        Long withdrawHistoryId = getLastHistoryId(marketingTerm);

        // when
        TermAgreementResponse result = termService.agreeTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isTrue(),
            () -> assertThat(result.agreedAt()).isNotNull(),
            () -> assertThat(result.isReagreementRequired()).isFalse(),
            () -> assertThat(getLastHistoryId(marketingTerm)).isNotEqualTo(withdrawHistoryId)
        );
    }

    @DisplayName("이미 철회한 약관을 다시 철회해도 이력을 쌓지 않고 성공한다")
    @Test
    void withdrawAlreadyWithdrawnTerm() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));
        termService.withdrawTerm(host, marketingTerm.getId());
        Long withdrawHistoryId = getLastHistoryId(marketingTerm);

        // when
        TermAgreementResponse result = termService.withdrawTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isFalse(),
            () -> assertThat(result.isReagreementRequired()).isFalse(),
            () -> assertThat(getLastHistoryId(marketingTerm)).isEqualTo(withdrawHistoryId)
        );
    }

    @DisplayName("개정으로 무효화된 선택 약관을 철회하면 철회 이력이 쌓여 재동의를 요구하지 않는다")
    @Test
    void withdrawInvalidatedOptionalTerm() {
        // given
        completeOnboarding();
        Term oldMarketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "old marketing"));
        Term latestMarketingTerm = termJpaRepository.save(createMarketingTerm("2.0.0", "2.0.0", "latest marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, oldMarketingTerm, AGREE)));
        Long agreeHistoryId = getLastHistoryId(latestMarketingTerm);

        // when
        TermAgreementResponse result = termService.withdrawTerm(host, latestMarketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isFalse(),
            () -> assertThat(result.isReagreementRequired()).isFalse(),
            () -> assertThat(getLastHistoryId(latestMarketingTerm)).isNotEqualTo(agreeHistoryId)
        );
    }

    @DisplayName("거절했던 선택 약관에 동의하면 동의 상태가 된다")
    @Test
    void agreeRejectedTerm() {
        // given
        completeOnboarding();
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        HostTermHistory rejected = new HostTermHistory(host, marketingTerm, REJECT);
        hostTermHistoryRepository.saveAll(List.of(rejected));

        // when
        TermAgreementResponse result = termService.agreeTerm(host, marketingTerm.getId());

        // then
        assertAll(
            () -> assertThat(result.isAgreed()).isTrue(),
            () -> assertThat(result.agreedAt()).isNotNull(),
            () -> assertThat(getLastHistoryId(marketingTerm)).isNotEqualTo(rejected.getId())
        );
    }

    @DisplayName("온보딩을 마치지 않은 호스트가 약관에 동의하면 예외를 던진다")
    @Test
    void agreeTermWithoutOnboarding() {
        // given
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));

        // when & then
        assertThatThrownBy(() -> termService.agreeTerm(host, marketingTerm.getId()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("온보딩이 완료되지 않은 호스트는 약관 동의 상태를 변경할 수 없습니다.")
            .extracting(exception -> ((BaseException)exception).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("온보딩을 마치지 않은 호스트가 약관 동의를 철회하면 예외를 던진다")
    @Test
    void withdrawTermWithoutOnboarding() {
        // given
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));

        // when & then
        assertThatThrownBy(() -> termService.withdrawTerm(host, marketingTerm.getId()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("온보딩이 완료되지 않은 호스트는 약관 동의 상태를 변경할 수 없습니다.")
            .extracting(exception -> ((BaseException)exception).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("해당 유형의 약관이 하나도 없으면 최신 약관 조회는 서버 오류로 처리한다")
    @Test
    void getLatestTermByTypeWithoutTerm() {
        // when & then
        assertThatThrownBy(() -> termJpaRepository.getLatestTermByTypeOrThrow(TermType.MARKETING))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("해당 유형의 약관이 존재하지 않습니다.")
            .extracting(exception -> ((BaseException)exception).getStatusCode())
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private Long getLastHistoryId(Term term) {
        return hostTermHistoryRepository.findLastHistoryByHostAndType(host, term.getType())
            .map(HostTermHistory::getId)
            .orElse(null);
    }

    /**
     * 토글 API는 온보딩을 마친 호스트만 사용할 수 있으므로, 필수 약관 동의 이력을 미리 적재해 둔다.
     * HostFixture.createHost()는 닉네임만 채워 주고 약관 동의 이력은 남기지 않는다.
     */
    private void completeOnboarding() {
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "onboarding service"));
        Term privacyTerm = termJpaRepository.save(createPrivacyTerm("1.0.0", "onboarding privacy"));
        hostTermHistoryRepository.saveAll(List.of(
            new HostTermHistory(host, serviceTerm, AGREE),
            new HostTermHistory(host, privacyTerm, AGREE)
        ));
    }
}
