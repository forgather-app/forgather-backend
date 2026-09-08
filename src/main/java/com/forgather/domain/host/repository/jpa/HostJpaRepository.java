package com.forgather.domain.host.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;

public interface HostJpaRepository extends JpaRepository<Host, Long>, HostRepository {
}
