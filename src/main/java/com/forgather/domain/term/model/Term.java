package com.forgather.domain.term.model;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "version", nullable = false, length = 30))
    private TermVersion version;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "min_agreed_version", nullable = false, length = 30))
    private TermVersion minAgreedVersion;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Term(
        TermType type,
        String name,
        String version,
        String minAgreedVersion,
        String content,
        Integer sortOrder
    ) {
        validateRequiredFields(type, name, version, minAgreedVersion, content, sortOrder);
        TermVersion parsedVersion = new TermVersion(version);
        TermVersion parsedMinAgreedVersion = new TermVersion(minAgreedVersion);
        validateVersions(parsedVersion, parsedMinAgreedVersion);
        this.type = type;
        this.name = name;
        this.version = parsedVersion;
        this.minAgreedVersion = parsedMinAgreedVersion;
        this.content = content;
        this.sortOrder = sortOrder;
    }

    private void validateRequiredFields(
        TermType type,
        String name,
        String version,
        String minAgreedVersion,
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
        if (minAgreedVersion == null) {
            throw new BaseNullPointerException("약관 최소 동의 버전은 null일 수 없습니다.");
        }
        if (content == null) {
            throw new BaseNullPointerException("약관 내용은 null일 수 없습니다.");
        }
        if (sortOrder == null) {
            throw new BaseNullPointerException("약관 정렬 순서는 null일 수 없습니다.");
        }
    }

    private void validateVersions(TermVersion version, TermVersion minAgreedVersion) {
        if (!version.isAtLeast(minAgreedVersion)) {
            throw new BaseException(
                "약관 최소 동의 버전은 약관 버전보다 클 수 없습니다. version: %s, minAgreedVersion: %s"
                    .formatted(version, minAgreedVersion));
        }
    }

    /**
     * 동의 당시 버전이 이 약관이 요구하는 최소 동의 버전 이상인지 판정한다.
     * 실질적 개정으로 minAgreedVersion이 상향되면 구버전 동의는 무효가 된다.
     */
    public boolean isAgreedVersionValid(TermVersion agreedVersion) {
        return agreedVersion.isAtLeast(minAgreedVersion);
    }
}
