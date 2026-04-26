package com.forgather.domain.guestbook.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGuestBookReportRequest(
    @NotNull
    Long reasonId,

    @Nullable
    @Size(min = 5, max = 100, message = "상세 사유는 최소 5자, 최대 200자까지 입력 가능합니다.")
    String detail
) {
}
