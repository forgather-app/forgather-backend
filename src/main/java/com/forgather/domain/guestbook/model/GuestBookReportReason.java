package com.forgather.domain.guestbook.model;

import lombok.Getter;

@Getter
public enum GuestBookReportReason {

    ADVERTISEMENT_SPAM("광고/홍보/스팸"),
    ABUSE_HARASSMENT("욕설/비방/괴롭힘"),
    HATE_DISCRIMINATION("차별/혐오 표현"),
    SEXUAL_CONTENT("음란/선정적 내용"),
    REPETITIVE_CONTENT("도배/반복 게시"),
    PERSONAL_INFORMATION_EXPOSURE("개인정보/민감정보 노출"),
    ILLEGAL_DANGEROUS_CONTENT("불법/위험 행위 또는 악성 링크"),
    OTHER("기타"),
    ;

    private final String label;

    GuestBookReportReason(String label) {
        this.label = label;
    }
}
