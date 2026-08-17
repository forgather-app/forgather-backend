package com.forgather.domain.term.dto;

import com.forgather.domain.term.model.Term;

import io.swagger.v3.oas.annotations.media.Schema;

public record TermResponse(

    @Schema(description = "약관 id", example = "1")
    Long id,

    @Schema(description = "약관 타입", example = "SERVICE")
    String type,

    @Schema(description = "약관 이름", example = "서비스 이용약관")
    String name,

    @Schema(description = "약관 버전", example = "1.0.0")
    String version,

    @Schema(description = "Markdown 형식의 약관 본문", example = "## 서비스 이용약관")
    String content,

    @Schema(description = "필수 동의 여부", example = "true")
    boolean isRequired,

    @Schema(description = "노출 순서", example = "1")
    int sortOrder
) {

    public static TermResponse from(Term term) {
        return new TermResponse(
            term.getId(),
            term.getType().name(),
            term.getName(),
            term.getVersion().getValue(),
            term.getContent(),
            term.getType().isRequired(),
            term.getSortOrder()
        );
    }
}
