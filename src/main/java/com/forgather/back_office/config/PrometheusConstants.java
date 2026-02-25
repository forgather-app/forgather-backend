package com.forgather.back_office.config;

/**
 * Prometheus 보안 모니터링에 사용되는 PromQL 쿼리 및 라벨 상수.
 */
public final class PrometheusConstants {

    private PrometheusConstants() {
    }

    /**
     * 오늘 하루 차단된 총 요청 수
     */
    public static final String TODAY_BLOCKED =
        "sum(increase(blocked_http_response_count_total[24h]))";

    /**
     * 차단 비율 (%) = 차단 / 전체 * 100
     */
    public static final String BLOCKED_RATIO = """
        sum(increase(blocked_http_response_count_total[24h]))
         / sum(increase(access_http_response_count_total[24h]))
         * 100
        """;

    /**
     * 429 Rate Limit 응답 수
     */
    public static final String RATE_LIMITED =
        "sum(increase(access_http_response_count_total{status=\"429\"}[24h]))";

    /**
     * 지난 24시간 내 새로 등장한 공격 IP 수
     */
    public static final String NEW_ATTACK_IPS = """
        count(
         count by (remote_addr)(increase(blocked_http_response_count_total[24h]) > 0)
          unless
         count by (remote_addr)(increase(blocked_http_response_count_total[24h] offset 1d) > 0)
        )
        """;

    /**
     * 차단되지 않은 4xx 응답 수 (403, 429 제외)
     */
    public static final String UNBLOCKED_4XX =
        "sum(increase(access_http_response_count_total{status=~\"4[0-9]{2}\",status!~\"403|429\"}[24h]))";

    /**
     * 공격 유형별 차단 수 (path_category 라벨 기준)
     */
    public static final String ATTACK_TYPES =
        "sum by (path_category)(increase(blocked_http_response_count_total[24h]))";

    /**
     * 상위 5개 공격 IP (remote_addr 라벨 기준)
     */
    public static final String TOP_ATTACK_IPS =
        "topk(5, sum by (remote_addr)(increase(blocked_http_response_count_total[24h])))";

    /**
     * nginx 차단 경로 분류 라벨
     */
    public static final String LABEL_PATH_CATEGORY = "path_category";

    /**
     * 클라이언트 IP 주소 라벨
     */
    public static final String LABEL_REMOTE_ADDR = "remote_addr";
}
