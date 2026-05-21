package com.forgather.domain.guestbook.model;

import static com.forgather.fixture.GuestBookReportReasonFixture.createReasonWithCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestBookReportReasonTest {

    @DisplayName("신고 사유는 노출 순서로 비교된다")
    @Test
    void compareToComparesDisplayOrder() {
        // given
        GuestBookReportReason firstReason = createReasonWithCode("SPAM", "스팸", 1);
        GuestBookReportReason secondReason = createReasonWithCode("ADULT", "성인 콘텐츠", 2);

        // when & then
        assertAll(
            () -> assertThat(firstReason.compareTo(secondReason)).isNegative(),
            () -> assertThat(secondReason.compareTo(firstReason)).isPositive()
        );
    }
}
