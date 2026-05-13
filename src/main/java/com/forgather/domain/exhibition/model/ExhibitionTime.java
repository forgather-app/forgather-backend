package com.forgather.domain.exhibition.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ExhibitionTime extends SoftDeleteEntity implements Comparable<ExhibitionTime> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public ExhibitionTime(Exhibition exhibition, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        validateRequiredFields(exhibition, dayOfWeek, startTime, endTime);
        validateTime(startTime, endTime);
        this.exhibition = exhibition;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private void validateRequiredFields(Exhibition exhibition, DayOfWeek dayOfWeek, LocalTime startTime,
        LocalTime endTime) {
        if (exhibition == null) {
            throw new BaseNullPointerException("전시는 null일 수 없습니다.");
        }
        if (dayOfWeek == null) {
            throw new BaseNullPointerException("운영 요일은 null일 수 없습니다.");
        }
        if (startTime == null) {
            throw new BaseNullPointerException("시작 시간은 null일 수 없습니다.");
        }
        if (endTime == null) {
            throw new BaseNullPointerException("종료 시간은 null일 수 없습니다.");
        }
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BaseException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
    }

    @Override
    public int compareTo(ExhibitionTime o) {
        return this.dayOfWeek.compareTo(o.dayOfWeek);
    }
}
