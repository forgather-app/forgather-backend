package com.forgather.global.auth.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;

public interface SpaceHostJpaRepository extends JpaRepository<SpaceHost, Long>, SpaceHostRepository {
}
