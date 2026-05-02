package com.forgather.back_office.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface AdminHostRepository {

    Optional<AppUser> findById(Long id);

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.name, h.createdAt,
                (SELECT COUNT(sh.id)
                 FROM SpaceHost sh JOIN sh.space s
                 WHERE sh.appUser = h AND s.deletedAt IS NULL AND sh.deletedAt IS NULL
                )
            )
            FROM AppUser h
            """,
        countQuery = "SELECT COUNT(h) FROM AppUser h"
    )
    Page<HostDetailResponse> findAllHostsWithSpaceCount(Pageable pageable);

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.name, h.createdAt,
                (SELECT COUNT(sh.id)
                 FROM SpaceHost sh JOIN sh.space s
                 WHERE sh.appUser = h AND s.deletedAt IS NULL AND sh.deletedAt IS NULL
                )
            )
            FROM AppUser h
            WHERE h.name LIKE CONCAT('%', :name, '%') ESCAPE '\\'
            """,
        countQuery = "SELECT COUNT(h) FROM AppUser h WHERE h.name LIKE CONCAT('%', :name, '%') ESCAPE '\\'"
    )
    Page<HostDetailResponse> findByNameContaining(
        @Param("name") String name,
        Pageable pageable
    );

    default AppUser getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("호스트의 id는 null일 수 없습니다. id: " + id);
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 호스트입니다. id: " + id));
    }
}
