package com.forgather.back_office.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.forgather.back_office.config.MonitoringProperties;
import com.forgather.back_office.dto.prometheus.PrometheusResponse;
import com.forgather.back_office.model.AttackIpInfo;
import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile(value = {"prod", "dev"})
@Service
public class PrometheusSecurityMetricsService implements SecurityMetricsService {

    private static final String QUERY_TODAY_BLOCKED =
        "sum(increase(blocked_http_response_count_total[24h]))";

    private static final String QUERY_BLOCKED_RATIO = """
        sum(increase(blocked_http_response_count_total[24h]))
         / sum(increase(access_http_response_count_total[24h]))
         * 100
        """;

    private static final String QUERY_RATE_LIMITED =
        "sum(increase(access_http_response_count_total{status=\"429\"}[24h]))";

    private static final String QUERY_NEW_ATTACK_IPS = """
        count(
         count by (remote_addr)(increase(blocked_http_response_count_total[24h]) > 0)
          unless
         count by (remote_addr)(increase(blocked_http_response_count_total[24h] offset 1d) > 0)
        )
        """;

    private static final String QUERY_UNBLOCKED_4XX =
        "sum(increase(access_http_response_count_total{status=~\"4[0-9]{2}\",status!~\"403|429\"}[24h]))";

    private static final String QUERY_ATTACK_TYPES =
        "sum by (path_category)(increase(blocked_http_response_count_total[24h]))";

    private static final String QUERY_TOP_ATTACK_IPS =
        "topk(5, sum by (remote_addr)(increase(blocked_http_response_count_total[24h])))";

    private static final SimpleClientHttpRequestFactory REQUEST_FACTORY;

    static {
        REQUEST_FACTORY = new SimpleClientHttpRequestFactory();
        REQUEST_FACTORY.setConnectTimeout(Duration.ofSeconds(3));
        REQUEST_FACTORY.setReadTimeout(Duration.ofSeconds(5));
    }

    private final RestClient restClient;

    public PrometheusSecurityMetricsService(MonitoringProperties properties) {
        this.restClient = RestClient.builder()
            .baseUrl(properties.prometheusUrl())
            .requestFactory(REQUEST_FACTORY)
            .build();
    }

    /**
     * 테스트용 패키지-프라이빗 생성자. WireMock 기반 RestClient를 직접 주입할 수 있다.
     */
    PrometheusSecurityMetricsService(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 7개 PromQL 쿼리를 순차 호출하여 보안 메트릭 요약을 반환한다.
     * <p>
     * all-or-nothing 전략: 어느 하나라도 실패하면 전체를 {@link SecuritySummary#empty()}로 반환한다.
     * 부분 성공 시 대시보드에 모순된 데이터가 표시되는 것을 방지하기 위함.
     */
    @Override
    public SecuritySummary getSummary() {
        try {
            long todayBlocked = queryScalar(QUERY_TODAY_BLOCKED);
            double blockedRatio = queryDouble(QUERY_BLOCKED_RATIO);
            long rateLimited = queryScalar(QUERY_RATE_LIMITED);
            long newAttackIps = queryScalar(QUERY_NEW_ATTACK_IPS);
            long unblocked4xx = queryScalar(QUERY_UNBLOCKED_4XX);
            Map<AttackType, Long> attackTypes = queryVectorAsAttackTypes(QUERY_ATTACK_TYPES);
            List<AttackIpInfo> topAttackIps = queryVectorAsAttackIps(QUERY_TOP_ATTACK_IPS);

            return new SecuritySummary(
                todayBlocked, blockedRatio, rateLimited,
                newAttackIps, unblocked4xx, attackTypes, topAttackIps
            );
        } catch (Exception e) {
            log.warn("Prometheus 보안 메트릭 조회 실패, 기본값 반환: {}", e.getMessage());
            return SecuritySummary.empty();
        }
    }

    /**
     * 스칼라 PromQL 쿼리를 실행하고 결과를 long으로 반환한다.
     * increase() 결과가 float(예: 1247.5)이므로 Math.round()로 변환한다.
     * 결과가 없으면(메트릭 미수집 등) 0을 반환한다.
     */
    private long queryScalar(String query) {
        PrometheusResponse response = executeQuery(query);
        if (isResponseNonExists(response)) {
            return 0L;
        }
        return Math.round(Double.parseDouble(String.valueOf(
            response.data()
                .result()
                .getFirst()
                .value()
                .get(1)
        )));
    }

    /**
     * 스칼라 PromQL 쿼리를 실행하고 결과를 소수점 1자리 double로 반환한다.
     * blockedRatio 전용. NaN 응답(분모 0) 시 Math.round(NaN)=0 → 0.0 반환.
     */
    private double queryDouble(String query) {
        PrometheusResponse response = executeQuery(query);
        if (isResponseNonExists(response)) {
            return 0.0;
        }
        double raw = Double.parseDouble(String.valueOf(
            response.data()
                .result()
                .getFirst()
                .value()
                .get(1)
        ));
        return Math.round(raw * 10.0) / 10.0;
    }

    /**
     * 벡터 PromQL 쿼리를 실행하고 path_category 라벨을 AttackType enum으로 매핑한다.
     * Prometheus label(credential_scan) → toUpperCase() → Java enum(CREDENTIAL_SCAN).
     * 예상 외 label 값은 {@link AttackType#OTHER}에 합산한다.
     */
    private Map<AttackType, Long> queryVectorAsAttackTypes(String query) {
        PrometheusResponse response = executeQuery(query);
        if (isResponseNonExists(response)) {
            return Map.of();
        }
        Map<AttackType, Long> attackTypes = new LinkedHashMap<>();
        for (PrometheusResponse.PrometheusResult result : response.data().result()) {
            String category = result.metric().getOrDefault("path_category", "other");
            long count = Math.round(Double.parseDouble(String.valueOf(result.value().get(1))));
            try {
                AttackType type = AttackType.valueOf(category.toUpperCase());
                attackTypes.put(type, count);
            } catch (IllegalArgumentException e) {
                attackTypes.merge(AttackType.OTHER, count, Long::sum);
            }
        }
        return attackTypes;
    }

    /**
     * 벡터 PromQL 쿼리를 실행하고 remote_addr 라벨을 AttackIpInfo로 변환한다.
     * topk(5, ...) PromQL이므로 결과는 상위 5개로 이미 제한되어 있다.
     */
    private List<AttackIpInfo> queryVectorAsAttackIps(String query) {
        PrometheusResponse response = executeQuery(query);
        if (isResponseNonExists(response)) {
            return List.of();
        }
        List<AttackIpInfo> attackIps = new ArrayList<>();
        for (PrometheusResponse.PrometheusResult result : response.data().result()) {
            String ip = result.metric().get("remote_addr");
            long count = Math.round(Double.parseDouble(String.valueOf(result.value().get(1))));
            attackIps.add(new AttackIpInfo(ip, count));
        }
        return attackIps;
    }

    private boolean isResponseNonExists(PrometheusResponse response) {
        return response == null ||
            response.data() == null ||
            response.data().result() == null ||
            response.data().result().isEmpty();
    }

    /**
     * Prometheus HTTP API(/api/v1/query)에 instant query를 실행한다.
     * URI 템플릿 변수로 PromQL 특수문자({}, =~ 등)가 자동 URL 인코딩된다.
     */
    private PrometheusResponse executeQuery(String query) {
        return restClient.get()
            .uri("/api/v1/query?query={query}", query)
            .retrieve()
            .body(PrometheusResponse.class);
    }
}
