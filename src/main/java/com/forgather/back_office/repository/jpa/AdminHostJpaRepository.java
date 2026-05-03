package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminHostRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.global.auth.model.Host;

public interface AdminHostJpaRepository extends ReadOnlyRepository<Host, Long>, AdminHostRepository {
}
