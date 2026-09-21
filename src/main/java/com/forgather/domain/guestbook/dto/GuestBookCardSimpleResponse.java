package com.forgather.domain.guestbook.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GuestBookCardSimpleResponse(
    @Schema(example = "1")
    Long id,

    @Schema(description = "방문자 닉네임", example = "밍퐁루블")
    String nickname,

    @Schema(description = "메세지", example = "전시 잘봤다~~ 너가 최고야 🤙")
    String message,

    @Schema(description = "방명록 카드 생성 시각", example = "2025-10-13T13:05")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime createdAt,

    @Schema(description = "사진 포함 여부", example = "false")
    Boolean containsPhoto,

    @Schema(description = "호스트 읽음 여부", example = "false")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isRead
) {

    public static GuestBookCardSimpleResponse from(GuestBookCardListDto guestBookCardDto) {
        return new GuestBookCardSimpleResponse(
            guestBookCardDto.id(),
            guestBookCardDto.nickname(),
            guestBookCardDto.message(),
            guestBookCardDto.createdAt(),
            guestBookCardDto.isPhotoExists(),
            null
        );
    }

    @Deprecated(forRemoval = true)
    public static GuestBookCardSimpleResponse fromWithReadStatus(GuestBookCardListDto guestBookCardDto) {
        return new GuestBookCardSimpleResponse(
            guestBookCardDto.id(),
            guestBookCardDto.nickname(),
            guestBookCardDto.message(),
            guestBookCardDto.createdAt(),
            guestBookCardDto.isPhotoExists(),
            guestBookCardDto.isRead()
        );
    }
}
