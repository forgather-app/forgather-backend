package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminSpaceHostMapRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.global.auth.model.SpaceHostMap;

public interface AdminSpaceHostMapJpaRepository
    extends ReadOnlyRepository<SpaceHostMap, Long>, AdminSpaceHostMapRepository {
}
