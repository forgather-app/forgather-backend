package com.forgather.domain.term.dto;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermAgreement;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;

class TermAgreementResponseTest {

    private static final LocalDateTime AGREED_AT = LocalDateTime.of(2026, 3, 1, 10, 0, 0);

    @DisplayName("최신 약관 정보와 도메인 판정 결과를 응답 필드로 매핑한다")
    @Test
    void mapFromTermAgreement() {
        // given
        Term latestTerm = createServiceTerm("2.0.0", "1.0.0", "latest service");
        Host host = HostFixture.createHost();
        HostTermHistory history = new HostTermHistory(host, createServiceTerm("1.0.0", "old service"), AGREE);
        ReflectionTestUtils.setField(history, "createdAt", AGREED_AT);
        TermAgreement agreement = new TermAgreement(latestTerm, history);

        // when
        TermAgreementResponse response = TermAgreementResponse.from(agreement);

        // then
        assertAll(
            () -> assertThat(response.id()).isEqualTo(latestTerm.getId()),
            () -> assertThat(response.type()).isEqualTo("SERVICE"),
            () -> assertThat(response.name()).isEqualTo(latestTerm.getName()),
            () -> assertThat(response.version()).isEqualTo("2.0.0"),
            () -> assertThat(response.content()).isEqualTo("latest service"),
            () -> assertThat(response.isRequired()).isTrue(),
            () -> assertThat(response.sortOrder()).isEqualTo(latestTerm.getSortOrder()),
            () -> assertThat(response.isAgreed()).isTrue(),
            () -> assertThat(response.agreedAt()).isEqualTo(AGREED_AT),
            () -> assertThat(response.isReagreementRequired()).isFalse()
        );
    }
}
