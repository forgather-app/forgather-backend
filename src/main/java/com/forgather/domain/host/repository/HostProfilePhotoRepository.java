package com.forgather.domain.host.repository;

import java.util.Optional;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.global.auth.model.Host;

public interface HostProfilePhotoRepository {

    HostProfilePhoto save(HostProfilePhoto hostProfilePhoto);

    Optional<HostProfilePhoto> findByHostAndDeletedAtIsNull(Host host);
}
