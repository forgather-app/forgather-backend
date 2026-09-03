package com.forgather.domain.host.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.forgather.domain.host.model.Host;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface HostRepository {

    Host save(Host host);

    Optional<Host> findById(Long id);

    Optional<Host> findByIdAndDeletedAtIsNull(Long id);

    Optional<Host> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCode(String code);

    /**
     * 호스트 행에 배타 락(SELECT ... FOR UPDATE)을 걸어 조회한다.
     * 프로필 사진처럼 호스트당 한 건만 유지해야 하는 자식 엔티티를 변경할 때,
     * 동시 요청이 같은 상태를 읽고 각각 새 행을 만드는 것을 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Host> findWithLockById(Long id);

    List<Host> findAllByDeletedAtBeforeAndAnonymizedAtIsNull(LocalDateTime threshold);

    Page<Host> findAll(Pageable pageable);

    default Host getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("호스트의 id는 null일 수 없습니다. id: " + id);
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 호스트입니다. id: " + id));
    }

    default Host getByIdWithLockOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("호스트의 id는 null일 수 없습니다. id: " + id);
        }
        return findWithLockById(id)
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

