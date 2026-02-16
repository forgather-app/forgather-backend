package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminSpaceHostMapRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.domain.space.model.Space;

public interface AdminSpaceHostMapJpaRepository extends ReadOnlyRepository<Space, Long>, AdminSpaceHostMapRepository {
}
