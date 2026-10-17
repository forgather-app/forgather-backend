package com.forgather.domain.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.space.model.Space;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface ProductRepository {
    Product save(Product product);

    List<Product> findAllBySpaceAndDeletedAtIsNull(Space space);

    /**
     * 스페이스의 대표 작품(가장 먼저 생성한 작품)을 조회한다.
     * createdAt이 밀리초 단위라 같은 배치에서 동점이 날 수 있어 id로 tie-break 한다.
     */
    Optional<Product> findFirstBySpaceAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Space space);

    /**
     * 여러 스페이스에서 각각 가장 먼저 생성된 작품을 한 번에 조회한다. 목록 조회의 N+1을 막기 위한 배치 쿼리다.
     * createdAt 동점이면 한 스페이스에서 2건 이상 나오므로 대표 작품 확정(id tie-break)은 호출부 책임이다.
     */
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.space.id IN :spaceIds
            AND p.deletedAt IS NULL
            AND p.createdAt = (
                SELECT MIN(p2.createdAt)
                FROM Product p2
                WHERE p2.space = p.space
                    AND p2.deletedAt IS NULL
            )
        ORDER BY p.id ASC
        """)
    List<Product> findEarliestCreatedPerSpace(@Param("spaceIds") List<Long> spaceIds);

    Optional<Product> findBySpaceAndIdAndDeletedAtIsNull(Space space, Long id);

    Long countBySpaceAndDeletedAtIsNull(Space space);

    default Product getBySpaceAndIdAndDeletedAtIsNullOrThrow(Space space, Long id) {
        if (space == null) {
            throw new BaseNullPointerException("스페이스는 null일 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (id == null) {
            throw new BaseNullPointerException("작품의 id는 null일 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return findBySpaceAndIdAndDeletedAtIsNull(space, id)
            .orElseThrow(() -> new NotFoundException("해당 스페이스에 존재하지 않는 작품입니다. spaceCode: %s, productId: %d"
                .formatted(space.getCode(), id)));
    }
}
