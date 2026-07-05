package com.forgather.domain.term.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermTypeTest {

    @DisplayName("필수 약관 타입은 enum에서 관리한다")
    @Test
    void requiredTypes() {
        // given, when
        Set<TermType> requiredTypes = TermType.requiredTypes();

        // then
        assertThat(requiredTypes).containsExactlyInAnyOrder(TermType.SERVICE, TermType.PRIVACY);
    }
}
