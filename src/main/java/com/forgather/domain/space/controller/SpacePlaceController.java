package com.forgather.domain.space.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.space.dto.PlaceSearchResponse;
import com.forgather.domain.space.service.PlaceSearchService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaces/places")
@Tag(name = "Space: 스페이스", description = "스페이스 관련 API")
public class SpacePlaceController {

    private final PlaceSearchService placeSearchService;

    @GetMapping
    @Operation(summary = "전시 장소 검색", description = "로그인한 호스트가 키워드로 장소를 검색합니다. "
        + "페이지당 최대 15건과 다음 페이지 존재 여부를 반환합니다. 결과가 없으면 빈 목록을 반환합니다. "
        + "장소 URL은 검색 제공자의 원본을 반환하며, 외부 서비스 장애 시 503을 반환합니다.")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> search(
        @LoginHost Host host,
        @Parameter(description = "검색 키워드", required = true, example = "성수 갤러리")
        @RequestParam(name = "keyword", required = false) String keyword,
        @Parameter(description = "페이지 번호 (1~45)", example = "1")
        @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        var response = placeSearchService.search(keyword, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
