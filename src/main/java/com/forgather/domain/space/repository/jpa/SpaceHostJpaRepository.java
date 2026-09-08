package com.forgather.domain.space.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.space.model.SpaceHost;
import com.forgather.domain.space.repository.SpaceHostRepository;

public interface SpaceHostJpaRepository extends JpaRepository<SpaceHost, Long>, SpaceHostRepository {
}
