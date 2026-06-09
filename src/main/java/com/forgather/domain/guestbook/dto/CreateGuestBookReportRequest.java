package com.forgather.domain.guestbook.dto;

import com.forgather.domain.guestbook.model.GuestBookReportReason;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateGuestBookReportRequest(
    @Schema(
        description = """
            신고 사유 코드
            - ADVERTISEMENT_SPAM: 광고/홍보/스팸
            - ABUSE_HARASSMENT: 욕설/비방/괴롭힘
            - HATE_DISCRIMINATION: 차별/혐오 표현
            - SEXUAL_CONTENT: 음란/선정적 내용
            - REPETITIVE_CONTENT: 도배/반복 게시
            - PERSONAL_INFORMATION_EXPOSURE: 개인정보/민감정보 노출
            - ILLEGAL_DANGEROUS_CONTENT: 불법/위험 행위 또는 악성 링크
            - OTHER: 기타
            """,
        example = "ADVERTISEMENT_SPAM",
        allowableValues = {
            "ADVERTISEMENT_SPAM",
            "ABUSE_HARASSMENT",
            "HATE_DISCRIMINATION",
            "SEXUAL_CONTENT",
            "REPETITIVE_CONTENT",
            "PERSONAL_INFORMATION_EXPOSURE",
            "ILLEGAL_DANGEROUS_CONTENT",
            "OTHER"
        },
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    GuestBookReportReason reason,
    String detail
) {
}
