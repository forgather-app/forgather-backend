package com.forgather.back_office.dto;

import com.forgather.global.validation.TextSize;

public record UpdateNoticeRequest(
    @TextSize(max = 100)
    String title,

    @TextSize(max = 10000)
    String body
) {
}
