package com.forgather.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public class SoftDeleteEntity extends BaseTimeEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void delete() {
        if (deletedAt != null) {
            return;
        }
        deletedAt = LocalDateTime.now();
    }

    /**
     * 논리 삭제를 되돌린다.
     * 같은 식별 값(예: 푸시 디바이스 토큰)이 다시 등록될 때 삭제된 행을 재사용하는 용도로만 쓰며,
     * 일반 복구 기능으로 노출하지 않기 위해 protected로 둔다.
     */
    protected void restore() {
        deletedAt = null;
    }
}
