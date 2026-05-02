package com.forgather.global.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.global.auth.model.KakaoAppUser;
import com.forgather.global.exception.NotFoundException;

public interface KakaoAppUserRepository extends JpaRepository<KakaoAppUser, Long> {

    Optional<KakaoAppUser> findByUserId(String userId);

    default KakaoAppUser getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new NotFoundException("KakaoAppUser를 찾을 수 없습니다. id: " + id));
    }
}
