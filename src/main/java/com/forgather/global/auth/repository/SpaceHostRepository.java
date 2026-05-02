package com.forgather.global.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;

import com.forgather.domain.space.model.Space;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface SpaceHostRepository {

    SpaceHost save(SpaceHost spaceHost);

    @Query("""
        SELECT sh
        FROM SpaceHost sh
            JOIN FETCH sh.space s
        WHERE sh.appUser = :appUser
            AND s.deletedAt IS NULL
            AND sh.deletedAt IS NULL
        ORDER BY s.createdAt DESC
        """)
    List<SpaceHost> findAllByAppUserAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(
        @Param("appUser") AppUser appUser
    );

    Optional<SpaceHost> findBySpaceAndAppUserAndDeletedAtIsNull(Space space, AppUser appUser);

    default SpaceHost getBySpaceAndAppUserAndDeletedAtIsNullOrThrow(Space space, AppUser appUser) {
        if (space == null) {
            throw new BaseNullPointerException("스페이스는 null일 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (appUser == null) {
            throw new BaseNullPointerException("유저는 null일 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return findBySpaceAndAppUserAndDeletedAtIsNull(space, appUser)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 자원입니다. spaceCode: %s, hostId: %d"
                .formatted(space.getCode(), appUser.getId())));
    }
}
