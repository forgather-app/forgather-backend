package com.forgather.domain.guestbook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.VisibilityStatus;
import com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto;
import com.forgather.domain.guestbook.repository.dto.SpaceGuestBookCountDto;
import com.forgather.domain.space.model.Space;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface GuestBookCardRepository {

    GuestBookCard save(GuestBookCard guestBookCard);

    Optional<GuestBookCard> findByIdAndDeletedAtIsNull(Long id);

    Long countBySpaceAndDeletedAtIsNull(Space space);

    Long countBySpaceAndVisibilityStatusAndDeletedAtIsNull(Space space, VisibilityStatus visibilityStatus);

    @Query("""
        SELECT new com.forgather.domain.guestbook.repository.dto.SpaceGuestBookCountDto(
            g.space.id,
            COUNT(g.id)
        )
        FROM GuestBookCard g
        WHERE g.space.id IN :spaceIds
            AND g.visibilityStatus = :visibilityStatus
            AND g.deletedAt IS NULL
        GROUP BY g.space.id
        """)
    List<SpaceGuestBookCountDto> countBySpaceIdInAndVisibilityStatusAndDeletedAtIsNull(
        @Param("spaceIds") List<Long> spaceIds,
        @Param("visibilityStatus") VisibilityStatus visibilityStatus
    );

    @Query("""
            SELECT new com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto(
                g.id,
                g.nickname,
                CASE WHEN (
                    SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g
                ) > 0 THEN true ELSE false END
            )
            FROM GuestBookCard g
            WHERE g.space = :space
                AND g.visibilityStatus = :visibilityStatus
                AND g.deletedAt IS NULL
        """)
    Page<GuestBookCardListDto> findAllDtoBySpaceAndVisibilityStatusAndDeletedAtIsNull(
        @Param("space") Space space,
        @Param("visibilityStatus") VisibilityStatus visibilityStatus,
        Pageable pageable
    );

    @Query("""
            SELECT new com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto(
                g.id,
                g.nickname,
                CASE WHEN (
                    SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g
                ) > 0 THEN true ELSE false END
            )
            FROM GuestBookCard g
            WHERE g.space = :space
                AND g.visibilityStatus = :visibilityStatus
                AND g.isRead = :isRead
                AND g.deletedAt IS NULL
        """)
    Page<GuestBookCardListDto> findAllDtoBySpaceAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
        @Param("space") Space space,
        @Param("visibilityStatus") VisibilityStatus visibilityStatus,
        @Param("isRead") boolean isRead,
        Pageable pageable
    );

    long countBySpaceAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
        Space space,
        VisibilityStatus visibilityStatus,
        boolean isRead
    );

    List<GuestBookCard> findAllBySpaceAndDeletedAtIsNull(Space space);

    long count();

    default GuestBookCard getByIdAndDeletedAtIsNullOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("방명록 카드의 id는 null일 수 없습니다.");
        }
        return findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 방명록 카드입니다. guestBookCardId: %d".formatted(id)));
    }
}
