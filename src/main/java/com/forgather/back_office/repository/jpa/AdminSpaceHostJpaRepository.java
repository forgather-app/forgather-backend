package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminSpaceHostRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.global.auth.model.SpaceHost;

public interface AdminSpaceHostJpaRepository
    extends ReadOnlyRepository<SpaceHost, Long>, AdminSpaceHostRepository {
}
