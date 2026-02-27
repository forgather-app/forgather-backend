package com.forgather.back_office.model;

import java.util.List;
import java.util.Map;

public record SecuritySummary(
    Long todayBlocked,
    Double blockedRatio,
    Long rateLimited,
    Long newAttackIps,
    Long unblocked4xx,
    Map<AttackType, Long> attackTypes,
    List<AttackIpInfo> topAttackIps
) {

    public SecuritySummary {
        attackTypes = attackTypes != null ? Map.copyOf(attackTypes) : null;
        topAttackIps = topAttackIps != null ? List.copyOf(topAttackIps) : null;
    }

    public static SecuritySummary empty() {
        return new SecuritySummary(null, null, null, null, null, null, null);
    }

    public boolean hasAnyData() {
        return todayBlocked != null || blockedRatio != null || rateLimited != null
            || newAttackIps != null || unblocked4xx != null
            || attackTypes != null || topAttackIps != null;
    }
}
