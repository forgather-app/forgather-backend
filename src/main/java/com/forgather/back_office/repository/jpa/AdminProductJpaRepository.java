package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminProductRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.domain.product.model.Product;

/**
 * AdminProductRepository의 Spring Data JPA 구현체.
 */
public interface AdminProductJpaRepository extends ReadOnlyRepository<Product, Long>, AdminProductRepository {
}
