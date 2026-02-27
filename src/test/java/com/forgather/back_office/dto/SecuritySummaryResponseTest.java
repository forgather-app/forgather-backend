package com.forgather.back_office.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.back_office.dto.SecuritySummaryResponse.AttackIpResponse;
import com.forgather.back_office.model.AttackIpInfo;
import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;

class SecuritySummaryResponseTest {

    @DisplayName("SecuritySummary를 SecuritySummaryResponse로 정상 변환한다")
    @Test
    void fromSummary() {
        // given
        SecuritySummary summary = new SecuritySummary(
            120L, 23.5, 3L, 18L, 12L,
            Map.of(AttackType.CREDENTIAL_SCAN, 50L, AttackType.CMS_SCAN, 30L),
            List.of(new AttackIpInfo("1.2.3.4", 10), new AttackIpInfo("5.6.7.8", 5))
        );

        // when
        SecuritySummaryResponse response = SecuritySummaryResponse.from(
            summary, "https://grafana.example.com", true
        );

        // then
        assertAll(
            () -> assertThat(response.todayBlocked()).isEqualTo(120),
            () -> assertThat(response.blockedRatio()).isEqualTo(23.5),
            () -> assertThat(response.rateLimited()).isEqualTo(3),
            () -> assertThat(response.newAttackIps()).isEqualTo(18),
            () -> assertThat(response.unblocked4xx()).isEqualTo(12),
            () -> assertThat(response.attackTypes()).containsEntry("credential_scan", 50L),
            () -> assertThat(response.attackTypes()).containsEntry("cms_scan", 30L),
            () -> assertThat(response.prometheusAvailable()).isTrue()
        );
    }

    @DisplayName("AttackIpInfo 목록을 AttackIpResponse 목록으로 변환한다")
    @Test
    void fromConvertsAttackIpInfoToAttackIpResponse() {
        // given
        SecuritySummary summary = new SecuritySummary(
            0L, 0.0, 0L, 0L, 0L, Map.of(),
            List.of(
                new AttackIpInfo("10.0.0.1", 100),
                new AttackIpInfo("10.0.0.2", 50),
                new AttackIpInfo("10.0.0.3", 25)
            )
        );

        // when
        SecuritySummaryResponse response = SecuritySummaryResponse.from(summary, "", false);

        // then
        List<AttackIpResponse> topAttackIps = response.topAttackIps();
        assertAll(
            () -> assertThat(topAttackIps).hasSize(3),
            () -> assertThat(topAttackIps.get(0).ip()).isEqualTo("10.0.0.1"),
            () -> assertThat(topAttackIps.get(0).count()).isEqualTo(100),
            () -> assertThat(topAttackIps.get(1).ip()).isEqualTo("10.0.0.2"),
            () -> assertThat(topAttackIps.get(1).count()).isEqualTo(50),
            () -> assertThat(topAttackIps.get(2).ip()).isEqualTo("10.0.0.3"),
            () -> assertThat(topAttackIps.get(2).count()).isEqualTo(25)
        );
    }

    @DisplayName("빈 SecuritySummary 입력 시 모든 필드가 null이고 available=false를 반환한다")
    @Test
    void fromEmptySummary() {
        // given
        SecuritySummary empty = SecuritySummary.empty();

        // when
        SecuritySummaryResponse response = SecuritySummaryResponse.from(empty, "", false);

        // then
        assertAll(
            () -> assertThat(response.todayBlocked()).isNull(),
            () -> assertThat(response.blockedRatio()).isNull(),
            () -> assertThat(response.rateLimited()).isNull(),
            () -> assertThat(response.newAttackIps()).isNull(),
            () -> assertThat(response.unblocked4xx()).isNull(),
            () -> assertThat(response.attackTypes()).isNull(),
            () -> assertThat(response.topAttackIps()).isNull(),
            () -> assertThat(response.prometheusAvailable()).isFalse()
        );
    }

    @DisplayName("부분 성공 시 실패 필드는 null로, 성공 필드는 정상 변환한다")
    @Test
    void fromPartialSummary() {
        // given
        SecuritySummary partial = new SecuritySummary(
            100L, null, 3L, null, null,
            Map.of(AttackType.CREDENTIAL_SCAN, 50L),
            null
        );

        // when
        SecuritySummaryResponse response = SecuritySummaryResponse.from(partial, "", true);

        // then
        assertAll(
            () -> assertThat(response.todayBlocked()).isEqualTo(100),
            () -> assertThat(response.blockedRatio()).isNull(),
            () -> assertThat(response.rateLimited()).isEqualTo(3),
            () -> assertThat(response.attackTypes()).containsEntry("credential_scan", 50L),
            () -> assertThat(response.topAttackIps()).isNull(),
            () -> assertThat(response.prometheusAvailable()).isTrue()
        );
    }

    @DisplayName("dashboardUrl 파라미터가 응답에 정확히 전달된다")
    @Test
    void fromPassesDashboardUrl() {
        // given
        String dashboardUrl = "https://grafana.forgather.com/d/security";
        SecuritySummary summary = SecuritySummary.empty();

        // when
        SecuritySummaryResponse response = SecuritySummaryResponse.from(
            summary, dashboardUrl, false
        );

        // then
        assertThat(response.grafanaDashboardUrl()).isEqualTo(dashboardUrl);
    }
}
