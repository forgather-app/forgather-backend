package com.forgather.domain.export.model;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

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
        validateRequiredField(jobId, "작업 ID");
        validateRequiredField(snapshot, "스냅샷");
        validateRequiredField(email, "이메일");
        this.jobId = jobId;
        this.snapshot = snapshot;
        this.email = email;
    }

    private void validateRequiredField(String value, String fieldName) {
        if (value == null) {
            throw new BaseNullPointerException("PDF 추출 항목(%s)에 null을 입력할 수 없습니다.".formatted(fieldName),
                HttpStatus.BAD_REQUEST);
        }
        if (value.isBlank()) {
            throw new BaseException("PDF 추출 항목(%s)에 공백만 입력할 수 없습니다.".formatted(fieldName));
        }
    }
}
