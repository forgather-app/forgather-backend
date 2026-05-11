package com.forgather.domain.exhibition.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;

public interface ExhibitionHostJpaRepository extends JpaRepository<ExhibitionHost, Long>, ExhibitionHostRepository {
}
