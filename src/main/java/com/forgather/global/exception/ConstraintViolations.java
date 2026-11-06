package com.forgather.global.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DB 제약 위반 예외가 특정 제약에서 났는지 판별한다.
 * 모든 {@link DataIntegrityViolationException}을 한 가지 의미(예: 중복)로 해석하면 같은 flush에 섞인
 * 다른 제약 위반이 가려지므로, 제약 이름을 확인한 뒤에만 도메인 예외로 변환한다.
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    /**
     * Hibernate가 추출한 제약 이름으로 판별한다. MySQL 8은 'table.constraint' 형태로 보고하므로 접미 일치도 허용한다.
     * 방언이 이름을 추출하지 못한 경우에만 드라이버 원본 메시지에 제약 이름이 포함되는지로 보조 판별한다.
     */
    public static boolean isViolationOf(DataIntegrityViolationException e, String constraintName) {
        if (e == null || constraintName == null) {
            return false;
        }
        String extracted = extractConstraintName(e);
        if (extracted != null) {
            return extracted.equals(constraintName) || extracted.endsWith("." + constraintName);
        }
        String rootMessage = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        return rootMessage != null && rootMessage.contains(constraintName);
    }

    private static String extractConstraintName(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
