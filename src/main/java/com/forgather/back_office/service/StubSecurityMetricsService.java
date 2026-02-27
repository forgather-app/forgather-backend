package com.forgather.back_office.service;

import static com.forgather.back_office.model.AttackType.CMS_SCAN;
import static com.forgather.back_office.model.AttackType.CREDENTIAL_SCAN;
import static com.forgather.back_office.model.AttackType.EXTENSION_SCAN;
import static com.forgather.back_office.model.AttackType.OTHER;
import static com.forgather.back_office.model.AttackType.RCE_SCAN;
import static com.forgather.back_office.model.AttackType.VCS_DEBUG_SCAN;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.forgather.back_office.model.AttackIpInfo;
import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;

/**
 * 로컬 개발을 위한 Stub Security Metrics Service
 * - 고정된 더미 데이터를 반환
 * - local, test 프로파일에서 활성화 (dev/prod는 PrometheusSecurityMetricsService 사용)
 */
@Profile("!prod & !dev")
@Service
public class StubSecurityMetricsService implements SecurityMetricsService {

    private static final long BASE_TODAY_BLOCKED = 1247;
    private static final double BASE_BLOCKED_RATIO = 23.4;
    private static final long BASE_RATE_LIMITED = 3;
    private static final long BASE_NEW_ATTACK_IPS = 18;
    private static final long BASE_UNBLOCKED_4XX = 12;

    @Override
    public SecuritySummary getSummary() {
        return new SecuritySummary(
            randomVariation(BASE_TODAY_BLOCKED, 0.1),
            roundToOneDecimal(randomVariation(BASE_BLOCKED_RATIO, 0.08)),
            randomVariation(BASE_RATE_LIMITED, 0.3),
            randomVariation(BASE_NEW_ATTACK_IPS, 0.15),
            randomVariation(BASE_UNBLOCKED_4XX, 0.2),
            createAttackTypes(),
            createTopAttackIps()
        );
    }

    private Map<AttackType, Long> createAttackTypes() {
        return Map.of(
            CREDENTIAL_SCAN, randomVariation(774, 0.1),
            CMS_SCAN, randomVariation(312, 0.1),
            VCS_DEBUG_SCAN, randomVariation(100, 0.15),
            EXTENSION_SCAN, randomVariation(37, 0.2),
            RCE_SCAN, randomVariation(15, 0.25),
            OTHER, randomVariation(9, 0.3)
        );
    }

    private List<AttackIpInfo> createTopAttackIps() {
        return List.of(
            new AttackIpInfo("135.125.133.168", randomVariation(342, 0.1)),
            new AttackIpInfo("209.38.91.12", randomVariation(128, 0.1)),
            new AttackIpInfo("176.65.132.193", randomVariation(87, 0.15)),
            new AttackIpInfo("154.81.14.244", randomVariation(45, 0.2)),
            new AttackIpInfo("167.94.146.49", randomVariation(23, 0.25))
        );
    }

    private long randomVariation(long base, double variationRate) {
        double min = base * (1 - variationRate);
        double max = base * (1 + variationRate);
        return (long)ThreadLocalRandom.current()
            .nextDouble(min, max);
    }

    private double randomVariation(double base, double variationRate) {
        double min = base * (1 - variationRate);
        double max = base * (1 + variationRate);
        return ThreadLocalRandom.current()
            .nextDouble(min, max);
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
