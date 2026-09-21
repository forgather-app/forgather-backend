package com.forgather.domain.exhibition.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.exhibition.model.ExhibitionPhoto;
import com.forgather.domain.exhibition.repository.ExhibitionPhotoRepository;

public interface ExhibitionPhotoJpaRepository extends JpaRepository<ExhibitionPhoto, Long>, ExhibitionPhotoRepository {
}
