package com.forgather.domain.exhibition.repository;

import java.util.List;

import com.forgather.domain.exhibition.model.ExhibitionTime;

public interface ExhibitionTimeRepository {

    <S extends ExhibitionTime> List<S> saveAll(Iterable<S> entities);
}
