package com.forgather.domain.guestbook.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.space.model.Space;

public interface GuestBookCardPhotoRepository {
    <S extends GuestBookCardPhoto> List<S> saveAll(Iterable<S> photos);

    List<GuestBookCardPhoto> findAllByGuestBookCardAndDeletedAtIsNull(GuestBookCard guestBookCard);

    /**
     * 스페이스의 미삭제 방명록 카드에 속한 사진을 벌크 UPDATE로 soft delete한다.
     * 엔티티를 로드하지 않으므로 {@code SoftDeleteEntity.delete()}와 auditing을 거치지 않는다.
     * updatedAt은 직접 세팅하며, 사진 삭제에 부가 로직이 생기면 이 경로도 함께 수정해야 한다.
     * 카드를 먼저 삭제하면 서브쿼리가 빈 집합이 되므로 반드시 카드 삭제보다 먼저 호출한다.
     * 같은 트랜잭션에서 먼저 로드한 엔티티에는 결과가 반영되지 않으므로, 대상 엔티티를 로드하기 전에 호출해야 한다.
     */
    @Modifying
    @Query("""
        UPDATE GuestBookCardPhoto p
        SET p.deletedAt = :now, p.updatedAt = :now
        WHERE p.deletedAt IS NULL
            AND p.guestBookCard.id IN (
                SELECT g.id FROM GuestBookCard g
                WHERE g.space = :space
                    AND g.deletedAt IS NULL
            )
        """)
    int softDeleteAllBySpace(@Param("space") Space space, @Param("now") LocalDateTime now);
}
