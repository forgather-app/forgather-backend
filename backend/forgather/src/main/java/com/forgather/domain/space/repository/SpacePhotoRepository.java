package com.forgather.domain.space.repository;

import java.util.List;
import java.util.Optional;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;

public interface SpacePhotoRepository {

    SpacePhoto save(SpacePhoto spacePhoto);

    Optional<SpacePhoto> findBySpaceAndDeletedAtIsNull(Space space);

    List<SpacePhoto> findAllBySpaceIdIn(List<Long> spaceIds);

    void delete(SpacePhoto spacePhoto);

    default SpacePhoto getBySpaceAndDeletedAtIsNullOrEmpty(Space space) {
        return findBySpaceAndDeletedAtIsNull(space)
            .orElse(SpacePhoto.empty(space));
    }
}
