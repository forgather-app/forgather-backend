package com.forgather.domain.space.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.space.dto.PublicHostSpacesResponse;
import com.forgather.domain.space.service.SpaceService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hosts/{hostCode}/spaces")
@Tag(name = "Space: 스페이스", description = "스페이스 관련 API")
public class PublicHostSpaceController {

    private final SpaceService spaceService;

    @GetMapping
    @Operation(summary = "호스트의 스페이스 목록 조회",
        description = "호스트 코드로 해당 호스트의 스페이스 목록을 조회합니다. 로그인이 필요하지 않습니다. "
            + "존재하지 않는 코드와 탈퇴한 호스트를 구분하지 않고 404로 응답합니다. "
            + "비공개 스페이스도 목록에 포함하며, 방명록 개수는 호스트 본인에게만 실제 값을 응답하고 "
            + "비로그인 사용자거나 호스트가 아니면 null(개수 비공개)로 응답합니다. 기본 정렬은 최신 생성 순입니다.")
    public ResponseEntity<ApiResponse<PublicHostSpacesResponse>> getPublicHostSpaces(
        @PathVariable(name = "hostCode") String hostCode,
        @LoginHost(required = false) Host loginHost
    ) {
        var response = spaceService.getPublicHostSpaces(hostCode, loginHost);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
