package com.forgather.domain.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

public interface ProductPhotoRepository {

    ProductPhoto save(ProductPhoto photo);

    /**
     * 작품의 첫 번째 사진(정렬 순서가 가장 앞선 사진)을 조회한다.
     */
    Optional<ProductPhoto> findFirstByProductAndDeletedAtIsNullOrderBySortOrderAsc(Product product);

    /**
     * 여러 작품의 첫 번째 사진을 한 번에 조회한다. 목록 조회의 N+1을 막기 위한 배치 쿼리다.
     */
    @Query("""
        SELECT ph
        FROM ProductPhoto ph
        WHERE ph.product.id IN :productIds
            AND ph.deletedAt IS NULL
            AND ph.sortOrder = (
                SELECT MIN(ph2.sortOrder)
                FROM ProductPhoto ph2
                WHERE ph2.product = ph.product
                    AND ph2.deletedAt IS NULL
            )
        """)
    List<ProductPhoto> findFirstPhotosByProductIdIn(@Param("productIds") List<Long> productIds);

    List<ProductPhoto> findAllByProductAndDeletedAtIsNull(Product product);

    <S extends ProductPhoto> List<S> saveAll(Iterable<S> photos);

}
