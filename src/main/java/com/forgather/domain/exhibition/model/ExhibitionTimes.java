package com.forgather.domain.exhibition.model;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.forgather.global.exception.BaseException;

import lombok.Getter;

@Getter
public class ExhibitionTimes {

    private final List<ExhibitionTime> values;

    public ExhibitionTimes(List<ExhibitionTime> exhibitionTimes) {
        validateDayOfWeekDuplicate(exhibitionTimes);
        this.values = exhibitionTimes.stream()
            .sorted()
            .toList();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private void validateDayOfWeekDuplicate(List<ExhibitionTime> exhibitionTimes) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (ExhibitionTime exhibitionTime : exhibitionTimes) {
            if (!seen.add(exhibitionTime.getDayOfWeek())) {
                throw new BaseException("전시 운영 시간에 같은 요일이 중복되었습니다.");
            }
        }
    }
}
