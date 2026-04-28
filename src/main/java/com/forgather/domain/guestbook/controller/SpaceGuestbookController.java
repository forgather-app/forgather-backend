package com.forgather.domain.guestbook.controller;

import static org.springframework.http.HttpStatus.CREATED;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.domain.guestbook.dto.CreateGuestBookReportRequest;
import com.forgather.domain.guestbook.dto.CreateGuestBookReportResponse;
import com.forgather.domain.guestbook.dto.DeleteGuestBookCardPhotosRequest;
import com.forgather.domain.guestbook.dto.GuestBookCardResponse;
import com.forgather.domain.guestbook.dto.GuestBookResponse;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardRequest;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardResponse;
import com.forgather.domain.guestbook.service.GuestbookReportService;
import com.forgather.domain.guestbook.service.GuestBookService;
import com.forgather.global.auth.annotation.LoginHost;
import com.forgather.global.auth.model.Host;
import com.forgather.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "SpaceGuestbook: 스페이스 방명록", description = "스페이스 내 방명록 관련 API")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceCode}/guestbook")
@RestController
public class SpaceGuestbookController {

    private final GuestBookService guestBookService;
    private final GuestbookReportService guestBookReportService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 조회",
        description = "공개 스페이스가 아닌 경우 호스트만 조회 가능, 방문자는 isRead 필드 누락",
        parameters = {
            @Parameter(
                name = "page",
                description = "페이지 번호 (1부터 시작)",
                schema = @Schema(type = "integer", minimum = "1"),
                example = "1"
            ),
            @Parameter(
                name = "size",
                description = "페이지 크기",
                schema = @Schema(type = "integer", minimum = "1"),
                example = "15"
            ),
            @Parameter(
                name = "sort",
                description = "정렬 조건 (여러개 지정 가능) (id,desc 포함 필수)",
                example = "createdAt,desc",
                array = @ArraySchema(schema = @Schema(type = "string"))
            )
        }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<GuestBookResponse>> readGuestBook(
        @PathVariable(value = "spaceCode") String spaceCode,
        @Parameter(hidden = true)
        @PageableDefault(size = 15, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
        Pageable pageable,
        @LoginHost(required = false) Host host
    ) {
        GuestBookResponse response = guestBookService.read(host, spaceCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 카드 조회", description = "공개 스페이스가 아닌 경우 호스트만 조회 가능")
    @GetMapping("/{guestBookCardId}")
    public ResponseEntity<ApiResponse<GuestBookCardResponse>> readCard(
        @PathVariable(value = "spaceCode") String spaceCode,
        @PathVariable(value = "guestBookCardId") Long guestBookCardId,
        @LoginHost(required = false) Host host
    ) {
        GuestBookCardResponse response = guestBookService.readCard(host, spaceCode, guestBookCardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방명록 카드 작성", description = "방문자 닉네임(10자), 메세지(400자)")
    @PostMapping
    public ResponseEntity<ApiResponse<WriteGuestBookCardResponse>> writeCard(
        @PathVariable(value = "spaceCode") String spaceCode,
        @RequestBody WriteGuestBookCardRequest request
    ) {
        var response = guestBookService.writeCard(spaceCode, request);
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 카드 삭제")
    @DeleteMapping("/{guestBookCardId}")
    public ResponseEntity<Void> deleteCard(
        @PathVariable(value = "spaceCode") String spaceCode,
        @PathVariable(value = "guestBookCardId") Long guestBookCardId,
        @LoginHost(required = true) Host host
    ) {
        guestBookService.deleteCard(host, spaceCode, guestBookCardId);
        return ResponseEntity.noContent().build();
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 신고", description = "호스트가 자신의 스페이스에 작성된 방명록을 신고한다. 신고 즉시 해당 방명록은 숨김 처리된다.")
    @PostMapping("/{guestBookCardId}/reports")
    public ResponseEntity<CreateGuestBookReportResponse> report(
        @PathVariable(value = "spaceCode") String spaceCode,
        @PathVariable(value = "guestBookCardId") Long guestBookCardId,
        @LoginHost(required = true) Host host,
        @RequestBody @Valid CreateGuestBookReportRequest request
    ) {
        var response = guestBookReportService.report(host, spaceCode, guestBookCardId, request);
        return ResponseEntity.status(CREATED).body(response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "방명록 카드 사진 선택 삭제")
    @DeleteMapping("/{guestBookCardId}/photos")
    public ResponseEntity<Void> deleteCardPhotos(
        @PathVariable(value = "spaceCode") String spaceCode,
        @PathVariable(value = "guestBookCardId") Long guestBookCardId,
        @RequestBody DeleteGuestBookCardPhotosRequest request,
        @LoginHost(required = true) Host host
    ) {
        guestBookService.deleteCardPhotos(host, spaceCode, guestBookCardId, request);
        return ResponseEntity.noContent().build();
    }
}
