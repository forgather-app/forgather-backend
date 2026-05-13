package com.forgather.domain.exhibition.controller;

import static org.springframework.http.HttpStatus.CREATED;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.service.ExhibitionService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Exhibition: 전시", description = "전시 관련 API")
@RequiredArgsConstructor
@RequestMapping("/exhibitions")
@RestController
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "전시 생성", description = "새로운 전시를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ExhibitionResponse>> create(
        @LoginHost(required = true) Host host,
        @Valid @RequestBody CreateExhibitionRequest request
    ) {
        var response = exhibitionService.create(host, request);
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }
}
