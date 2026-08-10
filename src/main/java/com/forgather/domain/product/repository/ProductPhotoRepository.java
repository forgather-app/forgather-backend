package com.forgather.domain.product.repository;

import java.util.List;
import java.util.Optional;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

public interface ProductPhotoRepository {

    ProductPhoto save(ProductPhoto photo);

    /**
     * 작품의 첫 번째 사진(정렬 순서가 가장 앞선 사진)을 조회한다. 정렬 순서가 같으면 id가 가장 작은 사진을 반환한다.
     */
    Optional<ProductPhoto> findFirstByProductAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Product product);

    /**
     * 여러 작품의 삭제되지 않은 사진을 한 번에 조회한다. 목록 조회의 N+1을 막기 위한 배치 쿼리다.
     * 작품당 사진은 최대 10장이라 과조회가 바운드되며, 첫 번째 사진 선정은 호출부가 담당한다.
     */
    List<ProductPhoto> findAllByProductIdInAndDeletedAtIsNull(List<Long> productIds);

    List<ProductPhoto> findAllByProductAndDeletedAtIsNull(Product product);

    <S extends ProductPhoto> List<S> saveAll(Iterable<S> photos);

}
