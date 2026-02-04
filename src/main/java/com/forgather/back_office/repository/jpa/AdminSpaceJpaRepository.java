package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminSpaceRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.domain.space.model.Space;

/**
 * AdminSpaceRepository의 Spring Data JPA 구현체.
 */
public interface AdminSpaceJpaRepository extends ReadOnlyRepository<Space, Long>, AdminSpaceRepository {
}
