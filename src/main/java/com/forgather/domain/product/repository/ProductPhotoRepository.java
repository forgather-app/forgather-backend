package com.forgather.domain.product.repository;

import java.util.List;
import java.util.Optional;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

public interface ProductPhotoRepository {

    ProductPhoto save(ProductPhoto photo);

    /**
     * 작품의 첫 번째 사진(정렬 순서가 가장 앞선 사진)을 조회한다.
     */
    Optional<ProductPhoto> findFirstByProductAndDeletedAtIsNullOrderBySortOrderAsc(Product product);

    List<ProductPhoto> findAllByProductAndDeletedAtIsNull(Product product);

    <S extends ProductPhoto> List<S> saveAll(Iterable<S> photos);

}
