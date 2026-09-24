package com.forgather.back_office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.SignupTrendResponse;
import com.forgather.back_office.model.MonthRange;
import com.forgather.back_office.model.TrendPoint;
import com.forgather.back_office.model.TrendUnit;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseException;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AdminSignupStatsServiceTest extends TestOnContainer {

    // 2026-09-23은 수요일
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);

    @Autowired
    private AdminSignupStatsService adminSignupStatsService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("일 단위로 KST 날짜 기준 가입 수를 집계하고 빈 날은 0으로 채운다.")
    @Test
    void dailyTrend() {
        // given
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 23, 0, 30));
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 22, 23, 59));
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 22, 9, 0));

        // when
        SignupTrendResponse result = adminSignupStatsService.getSignupTrend(TrendUnit.DAY, TODAY);

        // then
        assertAll(
            () -> assertThat(result.unit()).isEqualTo("DAY"),
            () -> assertThat(result.totalCount()).isEqualTo(3),
            () -> assertThat(result.points()).hasSize(30),
            () -> assertThat(result.points().get(0)).isEqualTo(new TrendPoint(LocalDate.of(2026, 8, 25), 0)),
            () -> assertThat(result.points().get(28)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 22), 2)),
            () -> assertThat(result.points().get(29)).isEqualTo(new TrendPoint(TODAY, 1))
        );
    }

    @DisplayName("주 단위는 일별 집계를 월요일 시작 주로 합산한다.")
    @Test
    void weeklyTrend() {
        // given
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 21, 10, 0)); // 월
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 23, 10, 0)); // 수
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 20, 10, 0)); // 지난주 일

        // when
        SignupTrendResponse result = adminSignupStatsService.getSignupTrend(TrendUnit.WEEK, TODAY);

        // then
        assertAll(
            () -> assertThat(result.points()).hasSize(12),
            () -> assertThat(result.points().get(11)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 21), 2)),
            () -> assertThat(result.points().get(10)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 14), 1))
        );
    }

    @DisplayName("월 단위는 일별 집계를 1일 시작 월로 합산한다.")
    @Test
    void monthlyTrend() {
        // given
        saveHostCreatedAt(LocalDateTime.of(2025, 10, 1, 0, 0));
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 1, 12, 0));

        // when
        SignupTrendResponse result = adminSignupStatsService.getSignupTrend(TrendUnit.MONTH, TODAY);

        // then
        assertAll(
            () -> assertThat(result.points()).hasSize(12),
            () -> assertThat(result.points().get(0)).isEqualTo(new TrendPoint(LocalDate.of(2025, 10, 1), 1)),
            () -> assertThat(result.points().get(11)).isEqualTo(new TrendPoint(LocalDate.of(2026, 9, 1), 1))
        );
    }

    @DisplayName("탈퇴한 회원도 가입 수에 포함한다.")
    @Test
    void includesWithdrawnHost() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        host.delete();
        setCreatedAt(host, LocalDateTime.of(2026, 9, 23, 10, 0));

        // when
        SignupTrendResponse result = adminSignupStatsService.getSignupTrend(TrendUnit.DAY, TODAY);

        // then
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @DisplayName("조회 범위 이전과 오늘 이후의 가입은 집계하지 않는다.")
    @Test
    void excludesOutOfRange() {
        // given
        saveHostCreatedAt(LocalDateTime.of(2026, 8, 24, 23, 59));
        saveHostCreatedAt(LocalDateTime.of(2026, 9, 24, 0, 0));

        // when
        SignupTrendResponse result = adminSignupStatsService.getSignupTrend(TrendUnit.DAY, TODAY);

        // then
        assertThat(result.totalCount()).isZero();
    }

    @DisplayName("지정한 월 기간의 가입 수를 월별로 집계하고, 기간 밖 가입은 제외한다.")
    @Test
    void monthlyTrendInRange() {
        // given
        saveHostCreatedAt(LocalDateTime.of(2026, 2, 28, 23, 59)); // 기간 이전
        saveHostCreatedAt(LocalDateTime.of(2026, 3, 1, 0, 0));
        saveHostCreatedAt(LocalDateTime.of(2026, 5, 31, 23, 59));
        saveHostCreatedAt(LocalDateTime.of(2026, 6, 1, 0, 0)); // 기간 이후
        MonthRange range = new MonthRange(YearMonth.of(2026, 3), YearMonth.of(2026, 5));

        // when
        SignupTrendResponse result = adminSignupStatsService.getMonthlySignupTrend(range, TODAY);

        // then
        assertAll(
            () -> assertThat(result.unit()).isEqualTo("MONTH"),
            () -> assertThat(result.totalCount()).isEqualTo(2),
            () -> assertThat(result.points()).containsExactly(
                new TrendPoint(LocalDate.of(2026, 3, 1), 1),
                new TrendPoint(LocalDate.of(2026, 4, 1), 0),
                new TrendPoint(LocalDate.of(2026, 5, 1), 1)
            )
        );
    }

    @DisplayName("종료 월이 이번 달보다 미래이면 예외가 발생한다.")
    @Test
    void monthlyTrendFutureMonth() {
        MonthRange range = new MonthRange(YearMonth.of(2026, 9), YearMonth.of(2026, 10));

        assertThatThrownBy(() -> adminSignupStatsService.getMonthlySignupTrend(range, TODAY))
            .isInstanceOf(BaseException.class);
    }

    @DisplayName("월 단위가 아니면 기간을 지정할 수 없다.")
    @Test
    void rangeOnlyForMonth() {
        assertThatThrownBy(() -> adminSignupStatsService.getSignupTrend(
            TrendUnit.DAY, YearMonth.of(2026, 3), YearMonth.of(2026, 5)))
            .isInstanceOf(BaseException.class);
    }

    private void saveHostCreatedAt(LocalDateTime kstDateTime) {
        Host host = hostRepository.save(HostFixture.createHost());
        setCreatedAt(host, kstDateTime);
    }

    private void setCreatedAt(Host host, LocalDateTime kstDateTime) {
        jdbcTemplate.update("UPDATE host SET created_at = ? WHERE id = ?", kstDateTime, host.getId());
    }
}
