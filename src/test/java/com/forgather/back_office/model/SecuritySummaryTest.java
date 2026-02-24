package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecuritySummaryTest {

    @DisplayName("empty()는 모든 값이 0 또는 빈 컬렉션인 SecuritySummary를 반환한다")
    @Test
    void empty() {
        // when
        SecuritySummary summary = SecuritySummary.empty();

        // then
        assertAll(
            () -> assertThat(summary.todayBlocked()).isZero(),
            () -> assertThat(summary.blockedRatio()).isZero(),
            () -> assertThat(summary.rateLimited()).isZero(),
            () -> assertThat(summary.newAttackIps()).isZero(),
            () -> assertThat(summary.unblocked4xx()).isZero(),
            () -> assertThat(summary.attackTypes()).isEmpty(),
            () -> assertThat(summary.topAttackIps()).isEmpty()
        );
    }

    @DisplayName("empty()를 두 번 호출하면 동일한 값을 가진 객체를 반환한다")
    @Test
    void emptyEquality() {
        // when
        SecuritySummary first = SecuritySummary.empty();
        SecuritySummary second = SecuritySummary.empty();

        // then
        assertThat(first).isEqualTo(second);
    }

    @DisplayName("값이 존재하는 SecuritySummary는 empty()와 다르다")
    @Test
    void nonEmptySummaryIsNotEqualToEmpty() {
        // given
        SecuritySummary populated = new SecuritySummary(
            100, 23.4, 3, 18, 12,
            Map.of(AttackType.CREDENTIAL_SCAN, 50L),
            List.of(new AttackIpInfo("1.2.3.4", 10))
        );

        // when & then
        assertThat(populated).isNotEqualTo(SecuritySummary.empty());
    }
}
