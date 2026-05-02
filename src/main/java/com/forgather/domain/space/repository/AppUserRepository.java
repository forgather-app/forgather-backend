package com.forgather.domain.space.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forgather.global.auth.model.AppUser;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface AppUserRepository {

    AppUser save(AppUser appUser);

    Optional<AppUser> findById(Long id);

    Page<AppUser> findAll(Pageable pageable);

    default AppUser getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("사용자의 id는 null일 수 없습니다. id: " + id);
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다. id: " + id));
    }
}
