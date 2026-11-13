package com.forgather.back_office.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;

import com.forgather.back_office.dto.AdminProductSummaryResponse;
import com.forgather.domain.product.model.Product;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

/**
 * 어드민 작품 조회 Repository.
 * 스페이스 구분 없이 활성 작품을 조회한다. 목록은 스페이스 정보와 사진 수를 DTO 프로젝션으로 한 번에 가져와 N+1을 막는다.
 * 작품과 스페이스 중 하나라도 논리 삭제되었으면 조회하지 않는다.
 */
public interface AdminProductRepository {

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.AdminProductSummaryResponse(
                p.id, p.title, p.authorName, s.code, s.name, p.videoUrl,
                (SELECT COUNT(ph.id)
                 FROM ProductPhoto ph
                 WHERE ph.product = p AND ph.deletedAt IS NULL
                ),
                p.createdAt, p.updatedAt
            )
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
            """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
            """
    )
    Page<AdminProductSummaryResponse> findAllActive(Pageable pageable);

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.AdminProductSummaryResponse(
                p.id, p.title, p.authorName, s.code, s.name, p.videoUrl,
                (SELECT COUNT(ph.id)
                 FROM ProductPhoto ph
                 WHERE ph.product = p AND ph.deletedAt IS NULL
                ),
                p.createdAt, p.updatedAt
            )
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
              AND s.code = :spaceCode
            """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
              AND s.code = :spaceCode
            """
    )
    Page<AdminProductSummaryResponse> findAllActiveBySpaceCode(
        @Param("spaceCode") String spaceCode,
        Pageable pageable
    );

    /**
     * 작품명 부분 일치로 활성 작품을 조회한다.
     *
     * @param title LIKE 와일드카드(\, %, _)를 이스케이프한 검색어
     */
    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.AdminProductSummaryResponse(
                p.id, p.title, p.authorName, s.code, s.name, p.videoUrl,
                (SELECT COUNT(ph.id)
                 FROM ProductPhoto ph
                 WHERE ph.product = p AND ph.deletedAt IS NULL
                ),
                p.createdAt, p.updatedAt
            )
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
              AND p.title LIKE CONCAT('%', :title, '%') ESCAPE '\\'
            """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
                JOIN p.space s
            WHERE p.deletedAt IS NULL
              AND s.deletedAt IS NULL
              AND p.title LIKE CONCAT('%', :title, '%') ESCAPE '\\'
            """
    )
    Page<AdminProductSummaryResponse> findAllActiveByTitleContaining(
        @Param("title") String title,
        Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Product p
            JOIN FETCH p.space s
        WHERE p.id = :productId
          AND p.deletedAt IS NULL
          AND s.deletedAt IS NULL
        """)
    Optional<Product> findActiveByIdWithSpace(@Param("productId") Long productId);

    default Product getActiveByIdWithSpaceOrThrow(Long productId) {
        if (productId == null) {
            throw new BaseNullPointerException("작품 id는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        return findActiveByIdWithSpace(productId)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 작품입니다. productId: " + productId));
    }
}
