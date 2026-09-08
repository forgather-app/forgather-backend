package com.forgather.domain.term.model;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.domain.term.model.HostTermHistoryAction.REJECT;
import static com.forgather.domain.term.model.HostTermHistoryAction.WITHDRAW;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.model.Host;
import com.forgather.fixture.HostFixture;

class TermAgreementTest {

    private static final LocalDateTime AGREED_AT = LocalDateTime.of(2026, 3, 1, 10, 0, 0);

    @DisplayName("필수 약관에 유효한 버전으로 동의했으면 동의 상태이고 재동의가 필요 없다")
    @Test
    void requiredTermAgreedWithValidVersion() {
        // given
        Term latestTerm = createServiceTerm("2.0.0", "1.0.0", "latest service");
        HostTermHistory history = history(createServiceTerm("1.0.0", "old service"), AGREE);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isTrue(),
            () -> assertThat(agreement.getAgreedAt()).isEqualTo(AGREED_AT),
            () -> assertThat(agreement.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("필수 약관이 실질 개정되어 동의 버전이 무효화되면 재동의가 필요하다")
    @Test
    void requiredTermAgreedWithInvalidVersion() {
        // given
        Term latestTerm = createServiceTerm("2.0.0", "2.0.0", "latest service");
        HostTermHistory history = history(createServiceTerm("1.0.0", "old service"), AGREE);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.getAgreedAt()).isNull(),
            () -> assertThat(agreement.isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("필수 약관을 철회했으면 재동의가 필요하다")
    @Test
    void requiredTermWithdrawn() {
        // given
        Term latestTerm = createServiceTerm("1.0.0", "service");
        HostTermHistory history = history(latestTerm, WITHDRAW);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.getAgreedAt()).isNull(),
            () -> assertThat(agreement.isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("필수 약관을 거절했으면 재동의가 필요하다")
    @Test
    void requiredTermRejected() {
        // given
        Term latestTerm = createServiceTerm("1.0.0", "service");
        HostTermHistory history = history(latestTerm, REJECT);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("필수 약관에 이력이 없으면 재동의가 필요하다")
    @Test
    void requiredTermWithoutHistory() {
        // given
        Term latestTerm = createServiceTerm("1.0.0", "service");

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, null);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.getAgreedAt()).isNull(),
            () -> assertThat(agreement.isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("선택 약관에 유효한 버전으로 동의했으면 동의 상태이고 재동의가 필요 없다")
    @Test
    void optionalTermAgreedWithValidVersion() {
        // given
        Term latestTerm = createMarketingTerm("1.0.0", "marketing");
        HostTermHistory history = history(latestTerm, AGREE);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isTrue(),
            () -> assertThat(agreement.getAgreedAt()).isEqualTo(AGREED_AT),
            () -> assertThat(agreement.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("선택 약관에 동의했지만 개정으로 무효화되면 재동의가 필요하다")
    @Test
    void optionalTermAgreementInvalidated() {
        // given
        Term latestTerm = createMarketingTerm("2.0.0", "2.0.0", "latest marketing");
        HostTermHistory history = history(createMarketingTerm("1.0.0", "old marketing"), AGREE);

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.isReagreementRequired()).isTrue()
        );
    }

    @DisplayName("선택 약관을 거절하거나 철회했으면 재동의를 요구하지 않는다")
    @Test
    void optionalTermRejectedOrWithdrawn() {
        // given
        Term latestTerm = createMarketingTerm("1.0.0", "marketing");

        // when
        TermAgreement rejected = new TermAgreement(latestTerm, history(latestTerm, REJECT));
        TermAgreement withdrawn = new TermAgreement(latestTerm, history(latestTerm, WITHDRAW));

        // then
        assertAll(
            () -> assertThat(rejected.isAgreed()).isFalse(),
            () -> assertThat(rejected.isReagreementRequired()).isFalse(),
            () -> assertThat(withdrawn.isAgreed()).isFalse(),
            () -> assertThat(withdrawn.isReagreementRequired()).isFalse()
        );
    }

    @DisplayName("선택 약관에 이력이 없으면 재동의를 요구하지 않는다")
    @Test
    void optionalTermWithoutHistory() {
        // given
        Term latestTerm = createMarketingTerm("1.0.0", "marketing");

        // when
        TermAgreement agreement = new TermAgreement(latestTerm, null);

        // then
        assertAll(
            () -> assertThat(agreement.isAgreed()).isFalse(),
            () -> assertThat(agreement.getAgreedAt()).isNull(),
            () -> assertThat(agreement.isReagreementRequired()).isFalse()
        );
    }

    private HostTermHistory history(Term term, HostTermHistoryAction action) {
        Host host = HostFixture.createHost();
        HostTermHistory history = new HostTermHistory(host, term, action);
        ReflectionTestUtils.setField(history, "createdAt", AGREED_AT);
        return history;
    }
}
