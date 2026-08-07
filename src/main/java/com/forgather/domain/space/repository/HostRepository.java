package com.forgather.domain.space.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface HostRepository {

    Host save(Host host);

    Optional<Host> findById(Long id);

    Optional<Host> findByIdAndDeletedAtIsNull(Long id);

    Optional<Host> findByCodeAndDeletedAtIsNull(String code);

    List<Host> findAllByDeletedAtBeforeAndAnonymizedAtIsNull(LocalDateTime threshold);

    Page<Host> findAll(Pageable pageable);

    default Host getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("호스트의 id는 null일 수 없습니다. id: " + id);
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 호스트입니다. id: " + id));
    }

    /**
     * 탈퇴하지 않은 호스트를 공개 코드로 조회한다.
     * 탈퇴한 호스트와 존재하지 않는 코드를 구분하지 않아 존재 여부가 드러나지 않는다.
     */
    default Host getActiveByCodeOrThrow(String code) {
        if (code == null) {
            throw new BaseNullPointerException("호스트 코드는 null일 수 없습니다.");
        }
        return findByCodeAndDeletedAtIsNull(code)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 호스트입니다. code: " + code));
    }
}

