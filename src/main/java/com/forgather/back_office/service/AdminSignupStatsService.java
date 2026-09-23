package com.forgather.back_office.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.SignupTrendResponse;
import com.forgather.back_office.model.DailyCount;
import com.forgather.back_office.model.MonthRange;
import com.forgather.back_office.model.TrendUnit;
import com.forgather.back_office.repository.AdminHostRepository;
import com.forgather.back_office.repository.DailyCountRow;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSignupStatsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AdminHostRepository adminHostRepository;

    /**
     * from/to가 없으면 단위별 기본 기간(오늘 포함)을 조회한다. 기간 지정은 월 단위에서만 가능하다.
     */
    public SignupTrendResponse getSignupTrend(TrendUnit unit, YearMonth from, YearMonth to) {
        LocalDate today = LocalDate.now(KST);
        if (from == null && to == null) {
            return getSignupTrend(unit, today);
        }
        if (unit != TrendUnit.MONTH) {
            throw new BaseException("기간 지정은 월 단위에서만 가능합니다. unit: " + unit);
        }
        return getMonthlySignupTrend(new MonthRange(from, to), today);
    }

    SignupTrendResponse getSignupTrend(TrendUnit unit, LocalDate today) {
        return aggregate(unit, unit.periodStarts(today), today.plusDays(1));
    }

    SignupTrendResponse getMonthlySignupTrend(MonthRange range, LocalDate today) {
        range.validateNotAfter(YearMonth.from(today));
        return aggregate(TrendUnit.MONTH, range.periodStarts(), range.endExclusive());
    }

    private SignupTrendResponse aggregate(TrendUnit unit, List<LocalDate> periodStarts, LocalDate endExclusive) {
        LocalDateTime from = toSystemDateTime(periodStarts.get(0));
        LocalDateTime to = toSystemDateTime(endExclusive);

        List<DailyCount> dailyCounts = adminHostRepository.countDailySignups(from, to, kstOffsetMinutes())
            .stream()
            .map(DailyCountRow::toDailyCount)
            .toList();
        return SignupTrendResponse.of(unit, unit.rollUp(dailyCounts, periodStarts));
    }

    // createdAt은 JPA Auditing이 JVM 기본 타임존으로 기록한다.
    private LocalDateTime toSystemDateTime(LocalDate kstDate) {
        return kstDate.atStartOfDay(KST)
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
    }

    // JVM 기본 타임존 기준 created_at을 KST로 옮기는 분 단위 오프셋 (JVM이 UTC면 540, KST면 0)
    private int kstOffsetMinutes() {
        Instant now = Instant.now();
        int kstSeconds = KST.getRules().getOffset(now).getTotalSeconds();
        int systemSeconds = ZoneId.systemDefault().getRules().getOffset(now).getTotalSeconds();
        return (kstSeconds - systemSeconds) / 60;
    }
}
