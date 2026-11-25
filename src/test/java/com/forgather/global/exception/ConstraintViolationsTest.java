package com.forgather.global.exception;

import static com.forgather.fixture.ConstraintViolationFixture.foreignKeyViolation;
import static com.forgather.fixture.ConstraintViolationFixture.uniqueViolation;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ConstraintViolationsTest {

    private static final String CONSTRAINT = "UK_notification_dedupe_key";

    @DisplayName("Hibernate가 추출한 제약 이름이 같으면 해당 제약 위반으로 판별한다")
    @Test
    void matchExactConstraintName() {
        // when & then
        assertThat(ConstraintViolations.isViolationOf(uniqueViolation(CONSTRAINT), CONSTRAINT)).isTrue();
    }

    @DisplayName("MySQL 8처럼 테이블 접두가 붙은 제약 이름도 해당 제약 위반으로 판별한다")
    @Test
    void matchTablePrefixedConstraintName() {
        // when & then
        assertThat(ConstraintViolations.isViolationOf(uniqueViolation("notification." + CONSTRAINT), CONSTRAINT))
            .isTrue();
    }

    @DisplayName("제약 이름이 다르면 해당 제약 위반으로 판별하지 않는다")
    @Test
    void rejectDifferentConstraintName() {
        // when & then
        assertThat(ConstraintViolations.isViolationOf(
            foreignKeyViolation("notification.FK_host_TO_notification"), CONSTRAINT)).isFalse();
    }

    @DisplayName("제약 이름을 추출하지 못하면 드라이버 메시지에 제약 이름이 포함되는지로 판별한다")
    @Test
    void fallBackToRootMessageWhenNameMissing() {
        // given
        SQLException sqlException = new SQLException("Duplicate entry 'k' for key 'notification." + CONSTRAINT + "'");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("could not execute statement",
            new ConstraintViolationException("could not execute statement", sqlException, ConstraintKind.UNIQUE, null));

        // when & then
        assertThat(ConstraintViolations.isViolationOf(exception, CONSTRAINT)).isTrue();
    }

    @DisplayName("원인 예외가 없으면 해당 제약 위반으로 판별하지 않는다")
    @Test
    void rejectWhenCauseMissing() {
        // when & then
        assertThat(ConstraintViolations.isViolationOf(new DataIntegrityViolationException("duplicate"), CONSTRAINT))
            .isFalse();
    }
}
