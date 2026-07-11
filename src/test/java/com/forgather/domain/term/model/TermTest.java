package com.forgather.domain.term.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseNullPointerException;

class TermTest {

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("약관 유형이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenTypeIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(null, "서비스 이용약관", "1.0.0", "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 유형은 null일 수 없습니다.");
        }

        @DisplayName("약관명이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenNameIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, null, "1.0.0", "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관명은 null일 수 없습니다.");
        }

        @DisplayName("약관 버전이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenVersionIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", null, "내용", 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 버전은 null일 수 없습니다.");
        }

        @DisplayName("약관 내용이 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenContentIsNull() {
            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", null, 1))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 내용은 null일 수 없습니다.");
        }

        @DisplayName("정렬 순서가 null이면 생성할 수 없다")
        @Test
        void throwExceptionWhenSortOrderIsNull() {
            // given
            Integer sortOrder = null;

            // when & then
            assertThatThrownBy(() -> new Term(TermType.SERVICE, "서비스 이용약관", "1.0.0", "내용", sortOrder))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("약관 정렬 순서는 null일 수 없습니다.");
        }
    }
}
