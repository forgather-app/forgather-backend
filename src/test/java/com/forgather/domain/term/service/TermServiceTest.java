package com.forgather.domain.term.service;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.dto.TermAgreementResponse;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;

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
}
