package com.forgather.back_office.repository.jpa;

import com.forgather.back_office.repository.AdminHostRepository;
import com.forgather.back_office.repository.ReadOnlyRepository;
import com.forgather.global.auth.model.AppUser;

public interface AdminHostJpaRepository extends ReadOnlyRepository<AppUser, Long>, AdminHostRepository {
}
