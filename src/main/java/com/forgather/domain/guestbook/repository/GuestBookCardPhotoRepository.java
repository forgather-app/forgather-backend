package com.forgather.domain.guestbook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.space.model.Space;

public interface GuestBookCardPhotoRepository {
    <S extends GuestBookCardPhoto> List<S> saveAll(Iterable<S> photos);

    List<GuestBookCardPhoto> findAllByGuestBookCardAndDeletedAtIsNull(GuestBookCard guestBookCard);

    /**
     * soft delete된 행을 포함해 여러 스페이스에 남겨진 모든 방명록 사진을 조회한다.
     */
    @Query("""
        SELECT p
        FROM GuestBookCardPhoto p
        WHERE p.guestBookCard.space IN :spaces
        """)
    List<GuestBookCardPhoto> findAllBySpaceIn(@Param("spaces") List<Space> spaces);
}
