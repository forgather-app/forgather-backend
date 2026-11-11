package com.forgather.domain.export.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.forgather.domain.model.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lambda가 저장한 PDF 추출 실패 이력을 백오피스에서 조회하는 엔티티.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePdfExportFailure extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String jobId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private PdfExportFailureStage stage;

    @Column(nullable = false, length = 512)
    private String functionArn;

    @Column(nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false, length = 64)
    private String failureCondition;

    private Integer approximateInvokeCount;

    @Column(length = 255)
    private String errorType;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String errorMessage;

    @Column(nullable = false, columnDefinition = "json")
    private String rawEvent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PdfExportFailureProcessingStatus processingStatus;

    @Column(columnDefinition = "TEXT")
    private String processingNote;

    @Column(nullable = false)
    private LocalDateTime failedAt;
}
