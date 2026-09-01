package com.forgather.domain.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.host.annotation.LoginHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.upload.dto.IssuePreSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlResponse;
import com.forgather.domain.upload.service.UploadService;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Upload: 파일 업로드", description = "파일 업로드 관련 API")
public class UploadController {

    private static final String PRESIGN_USAGE_NOTE = """
        
        
        ── 발급된 presigned URL 사용 시 주의사항 ──
        업로드 파일은 이미지만 허용됩니다. (확장자: webp)
        
        1. Content-Type 고정
           각 URL에는 파일 확장자로부터 결정된 Content-Type이 서명에 포함됩니다.
           PUT 요청 시 동일한 Content-Type 헤더를 보내야 합니다.
           (webp → image/webp)
        
        2. Content-Length 고정
           각 URL에는 요청 시 보낸 size(바이트)가 서명에 포함됩니다.
           PUT 요청 본문은 정확히 그 바이트 수여야 합니다. (파일당 최대 20MB)
        
        3. Content-Type 또는 크기가 일치하지 않으면 S3가 403(SignatureDoesNotMatch)을 반환합니다.
        """;

    private final UploadService uploadService;

    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    @PostMapping(path = "/spaces/{spaceCode}/upload/signed-urls")
    @Operation(summary = "업로드 URL 발급", description = """
            업로드 파일 별 서명된 URL을 발급합니다.
            category는 업로드할 사진의 종류를 뜻합니다.
            작품 사진 : PRODUCT
            방명록 사진 : GUESTBOOK
        """)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issuePreSignedUrls(
        @PathVariable(name = "spaceCode") String spaceCode,
        @RequestBody IssueSignedUrlRequest request
    ) {
        var response = uploadService.issueSignedUrls(spaceCode, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(path = "/spaces/{spaceCode}/guestbooks/upload/signed-urls")
    @Operation(summary = "스페이스 방명록 사진 업로드 URL 발급",
        description = "스페이스 방명록 사진 업로드용 presigned URL을 발급받습니다." + PRESIGN_USAGE_NOTE)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issueGuestbookPreSignedUrls(
        @PathVariable(name = "spaceCode") String spaceCode,
        @Valid @RequestBody IssuePreSignedUrlRequest request
    ) {
        var response = uploadService.issueGuestbookSignedUrls(spaceCode, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(path = "/spaces/{spaceCode}/products/upload/signed-urls")
    @Operation(summary = "스페이스 작품 사진 업로드 URL 발급",
        description = "로그인한 호스트가 자신의 스페이스 작품 사진 업로드용 presigned URL을 발급받습니다." + PRESIGN_USAGE_NOTE)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issueProductSignedUrls(
        @LoginHost(required = true) Host host,
        @PathVariable(name = "spaceCode") String spaceCode,
        @Valid @RequestBody IssuePreSignedUrlRequest request
    ) {
        var response = uploadService.issueProductSignedUrls(spaceCode, host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Deprecated
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(path = "/spaces/photos/upload/signed-urls")
    @Operation(summary = "스페이스 사진 업로드 URL 발급 (deprecated)",
        description = "[Deprecated] 스페이스 사진은 더 이상 별도로 업로드하지 않습니다. "
            + "스페이스 사진은 대표 작품의 첫 번째 사진을 사용하므로 이 API를 호출하지 마세요. "
            + "향후 기획 변경에 대비해 엔드포인트만 남겨둡니다." + PRESIGN_USAGE_NOTE)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issueSpacePhotoSignedUrls(
        @LoginHost(required = true) Host host,
        @Valid @RequestBody IssuePreSignedUrlRequest request
    ) {
        var response = uploadService.issueSpacePhotoSignedUrls(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(path = "/exhibitions/upload/signed-urls")
    @Operation(summary = "전시 사진 업로드 URL 발급",
        description = "로그인한 호스트가 전시 사진 업로드용 presigned URL을 발급받습니다." + PRESIGN_USAGE_NOTE)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issueExhibitionSignedUrls(
        @LoginHost(required = true) Host host,
        @Valid @RequestBody IssuePreSignedUrlRequest request
    ) {
        var response = uploadService.issueExhibitionSignedUrls(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(path = "/hosts/me/profile/upload/signed-urls")
    @Operation(summary = "프로필 사진 업로드 URL 발급",
        description = "로그인한 호스트가 자신의 프로필 사진 업로드용 presigned URL을 발급받습니다. "
            + "프로필 사진은 한 장만 발급할 수 있습니다." + PRESIGN_USAGE_NOTE)
    public ResponseEntity<ApiResponse<IssueSignedUrlResponse>> issueHostProfileSignedUrls(
        @LoginHost(required = true) Host host,
        @Valid @RequestBody IssuePreSignedUrlRequest request
    ) {
        var response = uploadService.issueHostProfileSignedUrls(host, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
