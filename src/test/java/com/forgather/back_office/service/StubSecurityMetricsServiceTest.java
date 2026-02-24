package com.forgather.back_office.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;

class StubSecurityMetricsServiceTest {

    private final StubSecurityMetricsService sut = new StubSecurityMetricsService();

    @DisplayName("오늘 차단 건수는 1,000에서 1,500 사이이다")
    @Test
    void todayBlockedIsInRange() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.todayBlocked()).isBetween(1000L, 1500L);
    }

    @DisplayName("차단율은 20%에서 27% 사이이다")
    @Test
    void blockedRatioIsInRange() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.blockedRatio()).isBetween(20.0, 27.0);
    }

    @DisplayName("Rate Limited 건수는 1에서 5 사이이다")
    @Test
    void rateLimitedIsInRange() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.rateLimited()).isBetween(1L, 5L);
    }

    @DisplayName("새로운 공격 IP 수는 14에서 22 사이이다")
    @Test
    void newAttackIpsIsInRange() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.newAttackIps()).isBetween(14L, 22L);
    }

    @DisplayName("차단되지 않은 4xx 건수는 8에서 16 사이이다")
    @Test
    void unblocked4xxIsInRange() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.unblocked4xx()).isBetween(8L, 16L);
    }

    @DisplayName("공격 유형 맵에는 6개의 카테고리가 포함된다")
    @Test
    void attackTypesContainsSixCategories() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.attackTypes()).containsOnlyKeys(
            AttackType.CREDENTIAL_SCAN,
            AttackType.CMS_SCAN,
            AttackType.VCS_DEBUG_SCAN,
            AttackType.EXTENSION_SCAN,
            AttackType.RCE_SCAN,
            AttackType.OTHER
        );
    }

    @DisplayName("공격 IP Top 5 목록은 정확히 5개의 항목을 포함한다")
    @Test
    void topAttackIpsHasFiveEntries() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.topAttackIps()).hasSize(5);
    }

    @DisplayName("연속 호출 시 서로 다른 값을 반환한다")
    @Test
    void consecutiveCallsReturnDifferentValues() {
        // when
        SecuritySummary first = sut.getSummary();
        SecuritySummary second = sut.getSummary();

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @DisplayName("반환된 SecuritySummary는 empty()와 다르다")
    @Test
    void summaryIsNotEmpty() {
        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary).isNotEqualTo(SecuritySummary.empty());
    }
}
