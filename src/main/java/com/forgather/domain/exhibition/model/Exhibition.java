package com.forgather.domain.exhibition.model;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Exhibition extends SoftDeleteEntity {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_OPERATION_NOTICE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "description")
    private String description;

    @Column(name = "operation_notice")
    private String operationNotice;

    @Embedded
    private Location location;

    public Exhibition(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String operationNotice,
        Location location
    ) {
        validateRequiredFields(title, startDate, endDate);
        validateTitle(title);
        validateOperationDate(startDate, endDate);
        validateDescription(description);
        validateOperationNotice(operationNotice);
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.operationNotice = operationNotice;
        this.location = location;
    }

    public ExhibitionStatus calculateProgressStatus(LocalDate standardDate) {
        return ExhibitionStatus.of(startDate, endDate, standardDate);
    }

    private void validateRequiredFields(String title, LocalDate startDate, LocalDate endDate) {
        if (title == null) {
            throw new BaseNullPointerException("전시 이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (startDate == null) {
            throw new BaseNullPointerException("전시 시작 날짜는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (endDate == null) {
            throw new BaseNullPointerException("전시 종료 날짜는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new BaseException("전시 이름은 공백일 수 없습니다.");
        }
        if (TextLengthCounter.count(title) > MAX_TITLE_LENGTH) {
            throw new BaseException("전시 이름은 %d자를 초과할 수 없습니다.".formatted(MAX_TITLE_LENGTH));
        }
    }

    private void validateOperationDate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BaseException("전시 시작 날짜는 종료 날짜 이후일 수 없습니다.");
        }
    }

    private void validateDescription(String description) {
        if (description == null) {
            return;
        }
        if (description.isBlank()) {
            throw new BaseException("전시 설명은 공백일 수 없습니다.");
        }
        if (TextLengthCounter.count(description) > MAX_DESCRIPTION_LENGTH) {
            throw new BaseException("전시 설명은 %d자를 초과할 수 없습니다.".formatted(MAX_DESCRIPTION_LENGTH));
        }
    }

    private void validateOperationNotice(String operationNotice) {
        if (operationNotice == null) {
            return;
        }
        if (operationNotice.isBlank()) {
            throw new BaseException("전시 운영 공지사항은 공백일 수 없습니다.");
        }
        if (TextLengthCounter.count(operationNotice) > MAX_OPERATION_NOTICE_LENGTH) {
            throw new BaseException("전시 운영 공지사항은 %d자를 초과할 수 없습니다.".formatted(MAX_OPERATION_NOTICE_LENGTH));
        }
    }
}
