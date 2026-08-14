package com.forgather.domain.space.repository;

import java.util.List;
import java.util.Optional;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;

public interface SpacePhotoRepository {

    SpacePhoto save(SpacePhoto spacePhoto);

    Optional<SpacePhoto> findBySpaceAndDeletedAtIsNull(Space space);

    List<SpacePhoto> findAllBySpaceIdInAndDeletedAtIsNull(List<Long> spaceIds);

    /**
     * soft delete된 행을 포함해 여러 스페이스의 모든 사진을 조회한다.
     */
    List<SpacePhoto> findAllBySpaceIn(List<Space> spaces);

    default SpacePhoto getBySpaceAndDeletedAtIsNullOrEmpty(Space space) {
        return findBySpaceAndDeletedAtIsNull(space)
            .orElse(SpacePhoto.empty(space));
    }
}
