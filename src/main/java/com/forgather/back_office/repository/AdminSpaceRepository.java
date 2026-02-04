package com.forgather.back_office.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;

import com.forgather.domain.space.model.Space;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

/**
 * 어드민 스페이스 조회 Repository.
 * 도메인 SpaceRepository와 달리 읽기 전용으로, 어드민 백오피스 스페이스 목록 조회 및 필터링에 사용한다.
 */
public interface AdminSpaceRepository {

    Optional<Space> findByCodeAndDeletedAtIsNull(String spaceCode);

    Page<Space> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * 작품 존재 여부로 필터링된 활성 스페이스 목록을 조회한다.
     *
     * @param hasProduct true면 작품이 있는 스페이스, false면 작품이 없는 스페이스
     * @param pageable   페이징 정보
     * @return 필터링된 스페이스 페이지
     */
    @Query("""
        SELECT DISTINCT s
        FROM Space s
                LEFT JOIN Product p ON p.space = s AND p.deletedAt IS NULL
        WHERE s.deletedAt IS NULL AND (
                (:hasProduct = true AND p.id IS NOT NULL) OR
                        (:hasProduct = false AND p.id IS NULL)
        )
        """)
    Page<Space> findActiveSpacesFilteredByProductExistence(
        @Param("hasProduct") boolean hasProduct,
        Pageable pageable
    );

    @Query("""
        SELECT s
        FROM Space s
        WHERE s.deletedAt IS NULL
          AND s.name LIKE CONCAT('%', :name, '%') ESCAPE '\\'
        """)
    Page<Space> findByNameContainingAndDeletedAtIsNull(
        @Param("name") String name,
        Pageable pageable
    );

    default Space getByCodeAndDeletedAtIsNullOrThrow(String spaceCode) {
        if (spaceCode == null) {
            throw new BaseNullPointerException("스페이스 코드는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        return findByCodeAndDeletedAtIsNull(spaceCode)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 스페이스입니다. spaceCode: " + spaceCode));
    }
}
