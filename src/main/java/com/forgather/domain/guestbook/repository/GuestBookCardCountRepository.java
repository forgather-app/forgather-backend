package com.forgather.domain.guestbook.repository;

public interface GuestBookCardCountRepository {

    /**
     * 논리 삭제된 데이터를 포함한 누적 전체 건수를 반환한다.
     */
    long count();
}
