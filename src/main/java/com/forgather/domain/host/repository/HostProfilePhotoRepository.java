package com.forgather.domain.host.repository;

import java.util.List;
import java.util.Optional;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.global.auth.model.Host;

public interface HostProfilePhotoRepository {

    HostProfilePhoto save(HostProfilePhoto hostProfilePhoto);

    Optional<HostProfilePhoto> findByHostAndDeletedAtIsNull(Host host);

    /**
     * soft delete된 행을 포함해 호스트의 모든 프로필 사진을 조회한다.
     */
    List<HostProfilePhoto> findAllByHost(Host host);
}
