package com.forgather.domain.exhibition.repository;

import java.util.List;

import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.host.model.Host;

public interface ExhibitionHostRepository {

    ExhibitionHost save(ExhibitionHost exhibitionHost);

    List<ExhibitionHost> findAllByHostAndDeletedAtIsNull(Host host);
}
