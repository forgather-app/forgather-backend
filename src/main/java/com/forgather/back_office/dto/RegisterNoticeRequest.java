package com.forgather.back_office.dto;

import com.forgather.domain.notice.model.Notice;
import com.forgather.global.validation.TextSize;

import jakarta.validation.constraints.NotBlank;

public record RegisterNoticeRequest(
    @NotBlank
    @TextSize(max = 100)
    String title,

    @NotBlank
    @TextSize(max = 10000)
    String body
) {

    public Notice toEntity() {
        return new Notice(title, body);
    }
}
