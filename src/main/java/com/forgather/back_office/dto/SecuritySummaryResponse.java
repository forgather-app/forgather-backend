package com.forgather.back_office.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.forgather.back_office.model.AttackIpInfo;
import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;

public record SecuritySummaryResponse(
    Long todayBlocked,
    Double blockedRatio,
    Long rateLimited,
    Long newAttackIps,
    Long unblocked4xx,
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
            mapAttackTypes(summary.attackTypes()),
            mapAttackIps(summary.topAttackIps()),
            dashboardUrl,
            available
        );
    }

    private static Map<String, Long> mapAttackTypes(Map<AttackType, Long> types) {
        if (types == null) {
            return null;
        }
        return types.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getKey(),
                Map.Entry::getValue
            ));
    }

    private static List<AttackIpResponse> mapAttackIps(List<AttackIpInfo> ips) {
        if (ips == null) {
            return null;
        }
        return ips.stream()
            .map(info -> new AttackIpResponse(info.getIp(), info.getCount()))
            .toList();
    }
}
