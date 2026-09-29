package com.forgather.domain.guestbook.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
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
        SELECT new com.forgather.domain.guestbook.repository.dto.SpaceGuestBookCountDto(
            g.space.id,
            COUNT(g.id)
        )
        FROM GuestBookCard g
        WHERE g.space.id IN :spaceIds
            AND g.visibilityStatus = :visibilityStatus
            AND g.isRead = :isRead
            AND g.deletedAt IS NULL
        GROUP BY g.space.id
        """)
    List<SpaceGuestBookCountDto> countBySpaceIdInAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
        @Param("spaceIds") List<Long> spaceIds,
        @Param("visibilityStatus") VisibilityStatus visibilityStatus,
        @Param("isRead") boolean isRead
    );

    @Query("""
            SELECT new com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto(
                g.id,
                g.nickname,
                g.message,
                g.createdAt,
                g.isRead,
                CASE WHEN (
                    SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g AND p.deletedAt IS NULL
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
                g.message,
                g.createdAt,
                g.isRead,
                CASE WHEN (
                    SELECT COUNT(p) FROM GuestBookCardPhoto p WHERE p.guestBookCard = g AND p.deletedAt IS NULL
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

    /**
     * 스페이스의 미삭제 방명록 카드를 벌크 UPDATE로 soft delete한다.
     * 엔티티를 로드하지 않으므로 {@code SoftDeleteEntity.delete()}와 auditing을 거치지 않는다.
     * updatedAt은 직접 세팅하며, {@code GuestBookCard.delete()}에 부가 로직이 생기면 이 경로도 함께 수정해야 한다.
     * 같은 트랜잭션에서 먼저 로드한 엔티티에는 결과가 반영되지 않으므로, 대상 엔티티를 로드하기 전에 호출해야 한다.
     */
    @Modifying
    @Query("""
        UPDATE GuestBookCard g
        SET g.deletedAt = :now, g.updatedAt = :now
        WHERE g.space = :space
            AND g.deletedAt IS NULL
        """)
    int softDeleteAllBySpace(@Param("space") Space space, @Param("now") LocalDateTime now);

    long count();

    default GuestBookCard getByIdAndDeletedAtIsNullOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("방명록 카드의 id는 null일 수 없습니다.");
        }
        return findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 방명록 카드입니다. guestBookCardId: %d".formatted(id)));
    }
}
