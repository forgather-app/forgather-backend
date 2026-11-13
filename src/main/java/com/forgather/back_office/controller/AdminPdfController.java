package com.forgather.back_office.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.dto.AdminPdfExportFailureDetailResponse;
import com.forgather.back_office.dto.AdminPdfExportFailureResponse;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.service.AdminPdfExportFailureService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/pdf")
@RequiredArgsConstructor
public class AdminPdfController {

    private final AdminPdfExportFailureService failureService;

    @GetMapping("/failures")
    public ResponseEntity<AdminPdfExportFailureResponse> getFailures(
        @PageableDefault(size = 15, sort = {"failedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
        @Admin AdminUser adminUser
    ) {
        var response = failureService.getFailures(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/failures/{id}")
    public ResponseEntity<AdminPdfExportFailureDetailResponse> getFailure(
        @PathVariable Long id,
        @Admin AdminUser adminUser
    ) {
        var response = failureService.getFailure(id);
        return ResponseEntity.ok(response);
    }
}
