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

@RestController
@RequestMapping("/admin/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final SecurityMetricsService securityMetricsService;
    private final MonitoringProperties monitoringProperties;

    @GetMapping("/summary")
    public ResponseEntity<SecuritySummaryResponse> getSummary(@Admin AdminUser adminUser) {
        SecuritySummary summary = securityMetricsService.getSummary();
        boolean available = !summary.equals(SecuritySummary.empty());
        return ResponseEntity.ok(
            SecuritySummaryResponse.from(summary, monitoringProperties.dashboardUrl(), available)
        );
    }
}
