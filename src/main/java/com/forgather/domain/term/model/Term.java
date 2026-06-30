package com.forgather.domain.term.model;

import com.forgather.domain.model.SoftDeleteEntity;

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

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    public Term(
        TermType type,
        String name,
        String version,
        String content,
        boolean isRequired,
        int sortOrder
    ) {
        this.type = type;
        this.name = name;
        this.version = version;
        this.content = content;
        this.isRequired = isRequired;
        this.sortOrder = sortOrder;
    }
}
