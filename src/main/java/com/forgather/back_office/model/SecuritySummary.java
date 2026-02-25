package com.forgather.back_office.model;

import java.util.List;
import java.util.Map;

public record SecuritySummary(
    long todayBlocked,
    double blockedRatio,
    long rateLimited,
    long newAttackIps,
    long unblocked4xx,
    Map<AttackType, Long> attackTypes,
    List<AttackIpInfo> topAttackIps
) {

    public SecuritySummary {
        attackTypes = Map.copyOf(attackTypes);
        topAttackIps = List.copyOf(topAttackIps);
    }

    public static SecuritySummary empty() {
        return new SecuritySummary(0, 0.0, 0, 0, 0, Map.of(), List.of());
    }
}
