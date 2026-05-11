package com.forgather.domain.exhibition.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.repository.ExhibitionRepository;

public interface ExhibitionJpaRepository extends JpaRepository<Exhibition, Long>, ExhibitionRepository {
}
