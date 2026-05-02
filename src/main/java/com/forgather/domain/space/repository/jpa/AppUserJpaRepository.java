package com.forgather.domain.space.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.global.auth.model.AppUser;

public interface AppUserJpaRepository extends JpaRepository<AppUser, Long>, AppUserRepository {
}
