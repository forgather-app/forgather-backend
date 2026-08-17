package com.forgather.domain.term.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약관 버전(semver) 값 객체. 상태는 "major.minor.patch" 문자열 하나다.
 * 문자열 비교로는 "10.0.0" < "2.0.0"이 되므로 비교 시점에 정수로 파싱한다.
 * 자릿수를 9자리로 제한해 Integer 오버플로를 막고, 선행 0을 금지해
 * 문자열 동등성과 버전 비교 결과가 어긋나지 않게 한다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermVersion implements Comparable<TermVersion> {

    private static final Pattern SEMVER_PATTERN =
        Pattern.compile("^(0|[1-9]\\d{0,8})\\.(0|[1-9]\\d{0,8})\\.(0|[1-9]\\d{0,8})$");

    private String value;

    public TermVersion(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null) {
            throw new BaseNullPointerException("약관 버전은 null일 수 없습니다.");
        }
        matchSemver(value);
    }

    private Matcher matchSemver(String value) {
        Matcher matcher = SEMVER_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new BaseException("약관 버전 형식이 올바르지 않습니다. version: " + value);
        }
        return matcher;
    }

    public boolean isAtLeast(TermVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(TermVersion other) {
        int[] parts = parse();
        int[] otherParts = other.parse();
        for (int i = 0; i < parts.length; i++) {
            int result = Integer.compare(parts[i], otherParts[i]);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * JPA 하이드레이션은 생성자를 거치지 않으므로 파싱 시점에도 형식을 방어한다.
     */
    private int[] parse() {
        Matcher matcher = matchSemver(value);
        return new int[] {
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TermVersion other)) {
            return false;
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
