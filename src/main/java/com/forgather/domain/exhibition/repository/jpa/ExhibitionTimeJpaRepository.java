package com.forgather.domain.exhibition.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.exhibition.model.ExhibitionTime;
import com.forgather.domain.exhibition.repository.ExhibitionTimeRepository;

public interface ExhibitionTimeJpaRepository extends JpaRepository<ExhibitionTime, Long>, ExhibitionTimeRepository {
}
