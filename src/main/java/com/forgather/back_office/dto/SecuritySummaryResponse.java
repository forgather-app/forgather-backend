package com.forgather.back_office.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.forgather.back_office.model.SecuritySummary;

public record SecuritySummaryResponse(
    long todayBlocked,
    double blockedRatio,
    long rateLimited,
    long newAttackIps,
    long unblocked4xx,
    Map<String, Long> attackTypes,
    List<AttackIpResponse> topAttackIps,
    String grafanaDashboardUrl,
    boolean prometheusAvailable
) {

    public record AttackIpResponse(String ip, long count) {
    }

    public static SecuritySummaryResponse from(
        SecuritySummary summary,
        String dashboardUrl,
        boolean available
    ) {
        return new SecuritySummaryResponse(
            summary.todayBlocked(),
            summary.blockedRatio(),
            summary.rateLimited(),
            summary.newAttackIps(),
            summary.unblocked4xx(),
            summary.attackTypes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().getKey(),
                    Map.Entry::getValue
                )),
            summary.topAttackIps().stream()
                .map(info -> new AttackIpResponse(info.getIp(), info.getCount()))
                .toList(),
            dashboardUrl,
            available
        );
    }
}
