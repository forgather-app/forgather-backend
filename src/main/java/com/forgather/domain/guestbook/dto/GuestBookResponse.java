package com.forgather.domain.guestbook.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

public record GuestBookResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "읽지 않은 방명록 카드 개수", example = "3")
    Long unreadCount,

    @Schema(description = "방명록 카드 목록", example = """
        [
            {
              "id": 1,
              "nickname": "밍퐁루블",
              "message": "전시 잘봤다~~ 너가 최고야 🤙",
              "createdAt": "2025-10-13T13:05",
              "containsPhoto": false
            },
            {
              "id": 2,
              "nickname": "레오",
              "message": "졸업 전시 축하해!",
              "createdAt": "2025-10-13T14:20",
              "containsPhoto": true
            },
            {
              "id": 3,
              "nickname": "포스티",
              "message": "전시 너무 잘 봤어 🤍",
              "createdAt": "2025-10-14T09:41",
              "containsPhoto": true
            }
          ]
        """)
    List<GuestBookCardSimpleResponse> guestBookCards,

    @Schema(description = "현재 페이지 번호", example = "1")
    int currentPage,

    @Schema(description = "페이지 당 방명록 카드 개수", example = "15")
    int pageSize,

    @Schema(description = "총 방명록 카드 개수", example = "3")
    long totalCount,

    @Schema(description = "총 페이지 수", example = "1")
    int totalPages
) {

    public GuestBookResponse(Page<GuestBookCardSimpleResponse> guestBookCards) {
        this(guestBookCards, null);
    }

    public GuestBookResponse(Page<GuestBookCardSimpleResponse> guestBookCards, Long unreadCount) {
        this(unreadCount,
            guestBookCards.toList(),
            guestBookCards.getNumber() + 1,
            guestBookCards.getSize(),
            guestBookCards.getTotalElements(),
            guestBookCards.getTotalPages());
    }
}
