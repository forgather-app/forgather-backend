package com.forgather.domain.term.model;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "term")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TermType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Term(
        TermType type,
        String name,
        String version,
        String content,
        Integer sortOrder
    ) {
        validateRequiredFields(type, name, version, content, sortOrder);
        this.type = type;
        this.name = name;
        this.version = version;
        this.content = content;
        this.sortOrder = sortOrder;
    }

    private void validateRequiredFields(
        TermType type,
        String name,
        String version,
        String content,
        Integer sortOrder
    ) {
        if (type == null) {
            throw new BaseNullPointerException("약관 유형은 null일 수 없습니다.");
        }
        if (name == null) {
            throw new BaseNullPointerException("약관명은 null일 수 없습니다.");
        }
        if (version == null) {
            throw new BaseNullPointerException("약관 버전은 null일 수 없습니다.");
        }
        if (content == null) {
            throw new BaseNullPointerException("약관 내용은 null일 수 없습니다.");
        }
        if (sortOrder == null) {
            throw new BaseNullPointerException("약관 정렬 순서는 null일 수 없습니다.");
        }
    }
}
