package com.forgather.back_office.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.config.MonitoringProperties;
import com.forgather.back_office.dto.SecuritySummaryResponse;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.model.SecuritySummary;
import com.forgather.back_office.service.SecurityMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final SecurityMetricsService securityMetricsService;
    private final MonitoringProperties monitoringProperties;

    @GetMapping("/summary")
    public ResponseEntity<SecuritySummaryResponse> getSummary(@Admin AdminUser adminUser) {
        SecuritySummary result;
        boolean available;
        try {
            result = securityMetricsService.getSummary();
            available = true;
        } catch (Exception e) {
            log.warn("보안 메트릭 조회 실패, 기본값 반환: {}", e.getMessage());
            result = SecuritySummary.empty();
            available = false;
        }
        return ResponseEntity.ok(
            SecuritySummaryResponse.from(result, monitoringProperties.dashboardUrl(), available)
        );
    }
}
