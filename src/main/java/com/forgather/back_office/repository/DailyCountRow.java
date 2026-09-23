package com.forgather.back_office.repository;

import java.time.LocalDate;

import com.forgather.back_office.model.DailyCount;

/**
 * 일별 집계 native query 결과. day는 'yyyy-MM-dd' 형식이다.
 */
public interface DailyCountRow {

    String getDay();

    Long getCount();

    default DailyCount toDailyCount() {
        return new DailyCount(LocalDate.parse(getDay()), getCount());
    }
}
