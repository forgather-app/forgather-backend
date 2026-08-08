package com.forgather.domain.space.model;

import com.forgather.domain.model.Photo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스페이스 사진은 더 이상 별도 업로드하지 않는다.
 * 조회 응답의 스페이스 사진은 대표 작품의 첫 번째 사진에서 계산한다. {@link com.forgather.domain.space.service.SpaceService} 참고.
 * <p>
 * 기존 운영 데이터가 남아 있고 향후 재사용 가능성이 있어 엔티티와 테이블을 존치한다.
 * 현재 프로덕션 사용처는 스페이스 삭제 시 정리 한 곳뿐이며, 어떤 API 응답에도 노출되지 않는다.
 */
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
