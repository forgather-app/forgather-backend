package com.forgather.domain.space.controller;

import static org.springframework.http.HttpStatus.CREATED;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.space.dto.CheckSpaceHostResponse;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.CreateSpaceResponse;
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.service.SpaceService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaces")
@Tag(name = "Space: 스페이스", description = "스페이스 관련 API")
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    @Operation(summary = "스페이스 생성", description = "새로운 스페이스를 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<CreateSpaceResponse>> create(
        @RequestBody @Valid CreateSpaceRequest request,
        @LoginHost Host host
    ) {
        var response = spaceService.create(request, host);
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    // 임시: FE ApiResponse 마이그레이션용 raw DTO 버전. v2/main 병합 전 반드시 삭제할 것. (#112)
    // 헤더 X-API-Version: 1 이 오면 raw DTO, 없으면 위의 기본(ApiResponse) 메서드가 처리한다.
    @PostMapping(headers = "X-API-Version=1")
    @Operation(summary = "스페이스 생성 (raw DTO)",
        description = "운영 호환 raw DTO 응답. X-API-Version: 1 로 호출. (FE 마이그레이션용 임시 버전 — v2/main 병합 전 삭제)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreateSpaceResponse> createV1(
        @RequestBody @Valid CreateSpaceRequest request,
        @LoginHost Host host
    ) {
        var response = spaceService.create(request, host);
        return ResponseEntity.status(CREATED).body(response);
    }

    @GetMapping("/{spaceCode}")
    @Operation(summary = "스페이스 조회", description = "스페이스 코드를 통해 스페이스 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<SpaceResponse>> getSpaceInformation(
        @PathVariable(name = "spaceCode") String spaceCode) {
        var response = spaceService.getSpaceInformation(spaceCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{spaceCode}")
    @Operation(summary = "스페이스 삭제", description = "스페이스를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable(name = "spaceCode") String spaceCode,
        @LoginHost Host host
    ) {
        spaceService.delete(spaceCode, host);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{spaceCode}")
    @Operation(summary = "스페이스 정보 수정", description = "해당 스페이스 코드의 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SpaceResponse>> update(
        @PathVariable(name = "spaceCode") String spaceCode,
        @RequestBody @Valid UpdateSpaceRequest request,
        @LoginHost Host host
    ) {
        var response = spaceService.update(spaceCode, request, host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 임시: FE ApiResponse 마이그레이션용 raw DTO 버전. v2/main 병합 전 반드시 삭제할 것. (#112)
    // 헤더 X-API-Version: 1 이 오면 raw DTO, 없으면 위의 기본(ApiResponse) 메서드가 처리한다.
    @PatchMapping(value = "/{spaceCode}", headers = "X-API-Version=1")
    @Operation(summary = "스페이스 정보 수정 (raw DTO)",
        description = "운영 호환 raw DTO 응답. X-API-Version: 1 로 호출. (FE 마이그레이션용 임시 버전 — v2/main 병합 전 삭제)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SpaceResponse> updateV1(
        @PathVariable(name = "spaceCode") String spaceCode,
        @RequestBody @Valid UpdateSpaceRequest request,
        @LoginHost Host host
    ) {
        var response = spaceService.update(spaceCode, request, host);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "호스트의 스페이스 목록 조회", description = "로그인한 호스트의 스페이스 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<HostSpaceResponse>> getSpacesInformation(@LoginHost Host host) {
        var response = spaceService.getSpacesInformation(host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/featured")
    @Operation(summary = "축하받는 스페이스 지정",
        description = "요청한 스페이스들을 '지금 축하받고 있는 스페이스'로 지정합니다. "
            + "요청에 포함되지 않은 스페이스의 지정 상태는 변경되지 않습니다."
            + "이미 지정된 스페이스가 포함되어 있어도 성공 응답을 반환합니다. "
            + "요청 목록에 호스트가 소유하지 않은 스페이스 코드가 하나라도 포함되면 부분 반영 없이 전체가 실패합니다. "
            + "존재하지 않는 코드와 다른 호스트의 코드를 구분하지 않고 400으로 응답합니다."
            + "응답에는 처리 후 지정된 호스트의 전체 스페이스 코드 목록이 담깁니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<FeaturedSpacesResponse>> featureSpaces(
        @RequestBody @Valid FeatureSpacesRequest request,
        @LoginHost Host host
    ) {
        var response = spaceService.featureSpaces(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me/featured")
    @Operation(summary = "축하받는 스페이스 지정 해제",
        description = "요청한 스페이스들의 '지금 축하받고 있는 스페이스' 지정을 해제합니다. "
            + "요청에 포함되지 않은 스페이스의 지정 상태는 변경되지 않습니다."
            + "이미 지정되지 않은 스페이스가 포함되어 있어도 성공 응답을 반환합니다. "
            + "요청 목록에 호스트가 소유하지 않은 스페이스 코드가 하나라도 포함되면 부분 반영 없이 전체가 실패합니다. "
            + "존재하지 않는 코드와 다른 호스트의 코드를 구분하지 않고 400으로 응답합니다."
            + "성공 시 data 없이 200으로 응답하며, 해제 후 지정 목록이 필요하면 GET /spaces/me 를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> unfeatureSpaces(
        @RequestBody @Valid UnfeatureSpacesRequest request,
        @LoginHost Host host
    ) {
        spaceService.unfeatureSpaces(host, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{spaceCode}/host-check")
    @Operation(summary = "스페이스의 호스트 여부 조회", description = "로그인한 호스트의 스페이스 호스트 여부를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<CheckSpaceHostResponse>> checkHost(
        @PathVariable(name = "spaceCode") String spaceCode,
        @LoginHost Host host
    ) {
        var response = spaceService.checkSpaceHost(spaceCode, host);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
