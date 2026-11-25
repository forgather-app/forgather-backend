package com.forgather.fixture;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Spring이 Hibernate 제약 위반을 변환한 형태를 흉내 낸다.
 * MySQL 8은 제약 이름을 'table.constraint'로 보고하므로 호출자가 그 형태로 넘긴다.
 */
public class ConstraintViolationFixture {

    private ConstraintViolationFixture() {
    }

    public static DataIntegrityViolationException uniqueViolation(String constraintName) {
        SQLException sqlException = new SQLException(
            "Duplicate entry 'x' for key '" + constraintName + "'", "23000", 1062);
        return wrap(sqlException, ConstraintKind.UNIQUE, constraintName);
    }

    public static DataIntegrityViolationException foreignKeyViolation(String constraintName) {
        SQLException sqlException = new SQLException(
            "Cannot add or update a child row: a foreign key constraint fails (CONSTRAINT `" + constraintName + "`)",
            "23000", 1452);
        return wrap(sqlException, ConstraintKind.OTHER, constraintName);
    }

    private static DataIntegrityViolationException wrap(
        SQLException sqlException,
        ConstraintKind kind,
        String constraintName
    ) {
        ConstraintViolationException cause =
            new ConstraintViolationException("could not execute statement", sqlException, kind, constraintName);
        return new DataIntegrityViolationException(cause.getMessage(), cause);
    }
}
