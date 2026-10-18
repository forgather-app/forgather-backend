package com.forgather.domain.space.model;

import java.time.LocalDate;
import java.util.Objects;

import com.forgather.global.exception.BaseException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceExhibitionPeriod {

    @Column(name = "exhibition_start_date", nullable = true)
    private LocalDate startDate;

    @Column(name = "exhibition_end_date", nullable = true)
    private LocalDate endDate;

    public SpaceExhibitionPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BaseException("전시 시작일과 종료일은 함께 입력해야 합니다. startDate: " + startDate
                + ", endDate: " + endDate);
        }
        if (startDate.isAfter(endDate)) {
            throw new BaseException("전시 시작일은 종료일보다 늦을 수 없습니다. startDate: " + startDate
                + ", endDate: " + endDate);
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SpaceExhibitionPeriod other)) {
            return false;
        }
        return Objects.equals(startDate, other.startDate) && Objects.equals(endDate, other.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }
}
