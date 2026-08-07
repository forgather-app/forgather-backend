package com.forgather.domain.space.model;

import com.forgather.domain.model.Photo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePhoto extends Photo {

    private static final String EMPTY_ORIGINAL_NAME = "";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    public SpacePhoto(Space space, String path, Long capacity) {
        super(EMPTY_ORIGINAL_NAME, path, capacity);
        this.space = space;
    }

    public static SpacePhoto empty(Space space) {
        return new SpacePhoto(space, "", 0L);
    }

    public boolean isExists() {
        return path != null && !path.isBlank();
    }
}
