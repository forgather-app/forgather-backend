package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecuritySummaryTest {

    @DisplayName("empty()는 모든 필드가 null인 SecuritySummary를 반환한다")
    @Test
    void empty() {
        // when
        SecuritySummary summary = SecuritySummary.empty();

        // then
        assertAll(
            () -> assertThat(summary.todayBlocked()).isNull(),
            () -> assertThat(summary.blockedRatio()).isNull(),
            () -> assertThat(summary.rateLimited()).isNull(),
            () -> assertThat(summary.newAttackIps()).isNull(),
            () -> assertThat(summary.unblocked4xx()).isNull(),
            () -> assertThat(summary.attackTypes()).isNull(),
            () -> assertThat(summary.topAttackIps()).isNull()
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
            100L, 23.4, 3L, 18L, 12L,
            Map.of(AttackType.CREDENTIAL_SCAN, 50L),
            List.of(new AttackIpInfo("1.2.3.4", 10))
        );

        // when & then
        assertThat(populated).isNotEqualTo(SecuritySummary.empty());
    }

    @DisplayName("하나라도 값이 있으면 hasAnyData()는 true를 반환한다")
    @Test
    void hasAnyDataWithPartialData() {
        // given
        SecuritySummary partial = new SecuritySummary(100L, null, null, null, null, null, null);

        // when & then
        assertThat(partial.hasAnyData()).isTrue();
    }

    @DisplayName("모든 필드가 null이면 hasAnyData()는 false를 반환한다")
    @Test
    void hasAnyDataAllNull() {
        // when & then
        assertThat(SecuritySummary.empty().hasAnyData()).isFalse();
    }
}
