package com.forgather.domain.exhibition.model;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.Photo;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ExhibitionPhoto extends Photo {

    private static final String EMPTY_ORIGINAL_NAME = "";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    public ExhibitionPhoto(String path, Long capacity, Exhibition exhibition) {
        super(EMPTY_ORIGINAL_NAME, path, capacity);
        validateRequiredFields(exhibition);
        this.exhibition = exhibition;
    }

    private void validateRequiredFields(Exhibition exhibition) {
        if (exhibition == null) {
            throw new BaseNullPointerException("전시는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
