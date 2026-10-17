package com.forgather.domain.host.repository;

public interface HostCountRepository {

    /**
     * 논리 삭제된 데이터를 포함한 누적 전체 건수를 반환한다.
     */
    long count();
}
