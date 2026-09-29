package com.forgather.back_office.controller;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.dto.SignupTrendResponse;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.model.TrendUnit;
import com.forgather.back_office.service.AdminSignupStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminSignupStatsService adminSignupStatsService;

    @GetMapping("/signups")
    public ResponseEntity<SignupTrendResponse> getSignupTrend(
        @RequestParam(name = "unit", defaultValue = "DAY") TrendUnit unit,
        @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
        @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
        @Admin AdminUser adminUser
    ) {
        var response = adminSignupStatsService.getSignupTrend(unit, from, to);
        return ResponseEntity.ok(response);
    }
}
