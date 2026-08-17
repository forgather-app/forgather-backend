package com.forgather.fixture;

import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;

public class TermFixture {

    /**
     * minAgreedVersion을 지정하지 않으면 자기 자신의 version을 쓴다 (V30 백필과 동일한 기본값).
     */
    public static Term createServiceTerm(String version, String content) {
        return createServiceTerm(version, version, content);
    }

    public static Term createServiceTerm(String version, String minAgreedVersion, String content) {
        return new Term(TermType.SERVICE, "서비스 이용약관", version, minAgreedVersion, content, 1);
    }

    public static Term createPrivacyTerm(String version, String content) {
        return createPrivacyTerm(version, version, content);
    }

    public static Term createPrivacyTerm(String version, String minAgreedVersion, String content) {
        return new Term(TermType.PRIVACY, "개인정보 수집 동의", version, minAgreedVersion, content, 2);
    }

    public static Term createMarketingTerm(String version, String content) {
        return createMarketingTerm(version, version, content);
    }

    public static Term createMarketingTerm(String version, String minAgreedVersion, String content) {
        return new Term(TermType.MARKETING, "마케팅 정보 수신 동의", version, minAgreedVersion, content, 3);
    }
}
