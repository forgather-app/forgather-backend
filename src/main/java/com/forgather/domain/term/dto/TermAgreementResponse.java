package com.forgather.domain.term.dto;

import java.time.LocalDateTime;

import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermAgreement;

import io.swagger.v3.oas.annotations.media.Schema;

public record TermAgreementResponse(

    @Schema(description = "약관 id", example = "7")
    Long id,

    @Schema(description = "약관 타입", example = "SERVICE")
    String type,

    @Schema(description = "약관 이름", example = "서비스 이용약관")
    String name,

    @Schema(description = "약관 버전", example = "2.0.0")
    String version,

    @Schema(description = "Markdown 형식의 약관 본문", example = "## 서비스 이용약관 (개정)")
    String content,

    @Schema(description = "필수 동의 여부", example = "true")
    boolean isRequired,

    @Schema(description = "노출 순서", example = "1")
    int sortOrder,

    @Schema(description = "현재 유효한 동의 여부", example = "true")
    boolean isAgreed,

    @Schema(description = "동의 일시. 유효한 동의가 없으면 null", example = "2026-03-01T10:00:00")
    LocalDateTime agreedAt,

    @Schema(description = "재동의 필요 여부. 필수 타입은 유효한 동의가 없으면 true, "
        + "선택 타입은 동의했었으나 개정으로 무효화된 경우에만 true", example = "false")
    boolean isReagreementRequired
) {

    public static TermAgreementResponse from(TermAgreement agreement) {
        Term latestTerm = agreement.getLatestTerm();
        return new TermAgreementResponse(
            latestTerm.getId(),
            latestTerm.getType().name(),
            latestTerm.getName(),
            latestTerm.getVersion().getValue(),
            latestTerm.getContent(),
            latestTerm.isRequiredType(),
            latestTerm.getSortOrder(),
            agreement.isAgreed(),
            agreement.getAgreedAt(),
            agreement.isReagreementRequired()
        );
    }
}
