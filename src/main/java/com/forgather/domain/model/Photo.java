package com.forgather.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Photo extends SoftDeleteEntity {

    private static final String ANONYMIZED_ORIGINAL_NAME = "익명화된 파일명";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "original_name", nullable = false)
    protected String originalName;

    @Column(name = "path", nullable = false)
    protected String path;

    @Column(name = "capacity", nullable = false)
    protected Long capacity; // bytes

    /**
     * 이용자가 올린 파일명에 개인정보가 섞일 수 있어 원본 파일명을 마스킹한다.
     */
    public void anonymize() {
        this.originalName = ANONYMIZED_ORIGINAL_NAME;
    }

    // TODO 검증 추가
    protected Photo(String originalName, String path, Long capacity) {
        this.originalName = originalName;
        this.path = path;
        this.capacity = capacity;
    }

    public Photo(Long id, String originalName, String path, Long capacity) {
        this.id = id;
        this.originalName = originalName;
        this.path = path;
        this.capacity = capacity;
    }
}
