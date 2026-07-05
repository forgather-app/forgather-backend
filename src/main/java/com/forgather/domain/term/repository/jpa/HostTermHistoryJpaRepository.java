package com.forgather.domain.term.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.repository.HostTermHistoryRepository;

public interface HostTermHistoryJpaRepository extends JpaRepository<HostTermHistory, Long>, HostTermHistoryRepository {
}
