package com.forgather.domain.term.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;

import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.model.TermType;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface TermRepository {

    @Query("""
        SELECT t
        FROM Term t
        WHERE t.id IN (
                SELECT MAX(latest.id)
                FROM Term latest
                WHERE latest.deletedAt IS NULL
                GROUP BY latest.type
            )
        ORDER BY t.sortOrder ASC
        """)
    List<Term> findLatestTerms();

    List<Term> findByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("""
        SELECT t
        FROM Term t
        WHERE t.id = (
            SELECT MAX(latest.id)
            FROM Term latest
            WHERE latest.type = :type
              AND latest.deletedAt IS NULL
        )
        """)
    Optional<Term> findLatestTermByType(@Param("type") TermType type);

    /**
     * 유형별 최신 약관은 해당 유형의 약관이 하나라도 있으면 항상 존재한다.
     * 비어 있다면 클라이언트 잘못이 아니라 약관 데이터 정합성이 깨진 상태이므로 서버 오류로 다룬다.
     */
    default Term getLatestTermByTypeOrThrow(TermType type) {
        if (type == null) {
            throw new BaseNullPointerException("약관 유형은 null일 수 없습니다.");
        }
        return findLatestTermByType(type)
            .orElseThrow(() -> new BaseException(
                "해당 유형의 약관이 존재하지 않습니다. type: " + type, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    Optional<Term> findByIdAndDeletedAtIsNull(Long id);

    default Term getByIdAndDeletedAtIsNullOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("약관 id는 null일 수 없습니다.");
        }
        return findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않거나 삭제된 약관입니다. termId: " + id));
    }
}
