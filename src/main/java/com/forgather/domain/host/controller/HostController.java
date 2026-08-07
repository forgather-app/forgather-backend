package com.forgather.domain.host.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.PublicHostProfileResponse;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.service.HostService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hosts")
@Tag(name = "Host: 호스트", description = "호스트 관련 API")
public class HostController {

    private final HostService hostService;

    @GetMapping("/me/profile")
    @Operation(summary = "내 프로필 조회",
        description = "로그인한 호스트의 프로필(닉네임, 한 줄 소개, 링크, 프로필 사진)을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HostProfileResponse>> getProfile(@LoginHost Host host) {
        var response = hostService.getProfile(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me/profile")
    @Operation(summary = "내 프로필 수정",
        description = "로그인한 호스트의 프로필을 수정합니다. "
            + "null인 필드는 변경하지 않고, 빈 문자열은 값을 제거합니다. (닉네임은 빈 문자열 불가)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HostProfileResponse>> updateProfile(
        @LoginHost Host host,
        @RequestBody UpdateHostProfileRequest request
    ) {
        var response = hostService.updateProfile(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{hostCode}/profile")
    @Operation(summary = "호스트 공개 프로필 조회",
        description = "호스트 코드로 공개 프로필(닉네임, 한 줄 소개, 링크, 프로필 사진)을 조회합니다. 로그인이 필요하지 않습니다. "
            + "존재하지 않는 코드와 탈퇴한 호스트를 구분하지 않고 404로 응답합니다.")
    public ResponseEntity<ApiResponse<PublicHostProfileResponse>> getPublicProfile(
        @PathVariable(name = "hostCode") String hostCode
    ) {
        var response = hostService.getPublicProfile(hostCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
