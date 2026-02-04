package com.forgather.back_office.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * 읽기 전용 Repository 기본 인터페이스.
 * CUD 메서드를 노출하지 않아 조회 전용 Repository 구현에 사용한다.
 */
@NoRepositoryBean
public interface ReadOnlyRepository<T, ID> extends Repository<T, ID> {

    Optional<T> findById(ID id);

    Page<T> findAll(Pageable pageable);
}
