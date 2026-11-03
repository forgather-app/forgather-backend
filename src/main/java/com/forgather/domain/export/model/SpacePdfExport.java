package com.forgather.domain.export.model;

import com.forgather.domain.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePdfExport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 255)
    private String jobId;

    @Column(nullable = false, columnDefinition = "json")
    private String snapshot;

    @Column(nullable = false, length = 320)
    private String email;

    public SpacePdfExport(String jobId, String snapshot, String email) {
        this.jobId = jobId;
        this.snapshot = snapshot;
        this.email = email;
    }
}
