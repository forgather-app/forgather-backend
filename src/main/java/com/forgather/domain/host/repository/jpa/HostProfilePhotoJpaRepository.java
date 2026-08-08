package com.forgather.domain.host.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;

public interface HostProfilePhotoJpaRepository
    extends JpaRepository<HostProfilePhoto, Long>, HostProfilePhotoRepository {
}
