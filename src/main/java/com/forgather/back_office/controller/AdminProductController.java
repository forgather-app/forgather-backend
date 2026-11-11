package com.forgather.back_office.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.dto.AdminProductDetailResponse;
import com.forgather.back_office.dto.AdminProductFilterRequest;
import com.forgather.back_office.dto.AdminProductResponse;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.service.AdminProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<AdminProductResponse> getAllProducts(
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
        Pageable pageable,
        @Admin AdminUser adminUser
    ) {
        var response = adminProductService.getAllProducts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<AdminProductResponse> getProductsByFilters(
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
        Pageable pageable,
        @ModelAttribute AdminProductFilterRequest request,
        @Admin AdminUser adminUser
    ) {
        var response = adminProductService.getProductsByFilters(request, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/by-title")
    public ResponseEntity<AdminProductResponse> getProductsByTitle(
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false, defaultValue = "") String title,
        @Admin AdminUser adminUser
    ) {
        var response = adminProductService.searchProductsByTitle(title, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<AdminProductDetailResponse> getProductDetail(
        @PathVariable(name = "productId") Long productId,
        @Admin AdminUser adminUser
    ) {
        var response = adminProductService.getProductDetail(productId);
        return ResponseEntity.ok(response);
    }
}
