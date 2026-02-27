package com.forgather.back_office.dto.prometheus;

import java.util.List;
import java.util.Map;

public record PrometheusResponse(
    String status,
    PrometheusData data
) {

    public record PrometheusData(
        String resultType,
        List<PrometheusResult> result
    ) {
    }

    public record PrometheusResult(
        Map<String, String> metric,
        List<Object> value
    ) {
    }
}
