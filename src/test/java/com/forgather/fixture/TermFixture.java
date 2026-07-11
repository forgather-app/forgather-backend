package com.forgather.fixture;

import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;

public class TermFixture {

    public static Term createServiceTerm(String version, String content) {
        return new Term(TermType.SERVICE, "서비스 이용약관", version, content, 1);
    }

    public static Term createPrivacyTerm(String version, String content) {
        return new Term(TermType.PRIVACY, "개인정보 수집 동의", version, content, 2);
    }

    public static Term createMarketingTerm(String version, String content) {
        return new Term(TermType.MARKETING, "마케팅 정보 수신 동의", version, content, 3);
    }
}
