package com.forgather.domain.term.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class TermTest {

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("약관 유형이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenTypeIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(null, "서비스 이용약관", "1.0.0", "1.0.0", "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 유형은 null일 수 없습니다.");
        }

        @DisplayName("약관명이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenNameIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, null, "1.0.0", "1.0.0", "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관명은 null일 수 없습니다.");
        }

        @DisplayName("약관 버전이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenVersionIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", null, "1.0.0", "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 버전은 null일 수 없습니다.");
        }

        @DisplayName("최소 동의 버전이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenMinAgreedVersionIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", null, "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 최소 동의 버전은 null일 수 없습니다.");
        }

        @DisplayName("약관 내용이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenContentIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", "1.0.0", null, 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 내용은 null일 수 없습니다.");
        }

        @DisplayName("정렬 순서가 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenSortOrderIsNull() {
            // given
            Integer sortOrder = null;

            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", "1.0.0", "내용", sortOrder))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 정렬 순서는 null일 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("버전 검증")
    class VersionValidation {

        @DisplayName("약관 버전이 semver 형식이 아니면 생성할 수 없다")
        @Test
        void throwExceptionWhenVersionIsNotSemver() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1", "1.0.0", "내용", 1))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("약관 버전 형식이 올바르지 않습니다.");
        }

        @DisplayName("최소 동의 버전이 semver 형식이 아니면 생성할 수 없다")
        @Test
        void throwExceptionWhenMinAgreedVersionIsNotSemver() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", "1.0", "내용", 1))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("약관 버전 형식이 올바르지 않습니다.");
        }

        @DisplayName("최소 동의 버전이 약관 버전보다 크면 생성할 수 없다")
        @Test
        void throwExceptionWhenMinAgreedVersionIsGreaterThanVersion() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", "2.0.0", "내용", 1))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("약관 최소 동의 버전은 약관 버전보다 클 수 없습니다.");
        }

        @DisplayName("최소 동의 버전이 약관 버전과 같으면 생성할 수 있다")
        @Test
        void createTermWhenMinAgreedVersionEqualsVersion() {
            // when
            Term term = new Term(TermType.SERVICE, "서비스 이용약관", "2.0.0", "2.0.0", "내용", 1);

            // then
            assertThat(term.getMinAgreedVersion()).isEqualTo(new TermVersion("2.0.0"));
        }
    }

    @Nested
    @DisplayName("동의 버전 유효성 판정")
    class AgreedVersionValidity {

        @DisplayName("동의한 버전이 최소 동의 버전 이상이면 유효하다")
        @Test
        void validWhenAgreedVersionIsNotLessThanMinAgreedVersion() {
            // given
            Term term = new Term(TermType.SERVICE, "서비스 이용약관", "2.0.0", "2.0.0", "내용", 1);

            // when & then
            assertAll(
                () -> assertThat(term.isAgreedVersionValid(new TermVersion("2.0.0"))).isTrue(),
                () -> assertThat(term.isAgreedVersionValid(new TermVersion("2.0.1"))).isTrue(),
                () -> assertThat(term.isAgreedVersionValid(new TermVersion("1.9.9"))).isFalse()
            );
        }

        @DisplayName("경미한 개정으로 최소 동의 버전이 유지되면 구버전 동의도 유효하다")
        @Test
        void validWhenMinAgreedVersionIsKept() {
            // given
            Term term = new Term(TermType.SERVICE, "서비스 이용약관", "1.1.0", "1.0.0", "내용", 1);

            // when & then
            assertAll(
                () -> assertThat(term.isAgreedVersionValid(new TermVersion("1.0.0"))).isTrue(),
                () -> assertThat(term.isAgreedVersionValid(new TermVersion("0.9.9"))).isFalse()
            );
        }
    }
}
