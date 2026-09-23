package com.forgather.back_office.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.domain.host.model.Host;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

public interface AdminHostRepository {

    Optional<Host> findById(Long id);

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.nickname, h.createdAt,
                (SELECT COUNT(sh.id)
                 FROM SpaceHost sh JOIN sh.space s
                 WHERE sh.host = h AND s.deletedAt IS NULL AND sh.deletedAt IS NULL
                )
            )
            FROM Host h
            """,
        countQuery = "SELECT COUNT(h) FROM Host h"
    )
    Page<HostDetailResponse> findAllHostsWithSpaceCount(Pageable pageable);

    @Query(
        value = """
            SELECT new com.forgather.back_office.dto.HostDetailResponse(
                h.id, h.nickname, h.createdAt,
                (SELECT COUNT(sh.id)
                 FROM SpaceHost sh JOIN sh.space s
                 WHERE sh.host = h AND s.deletedAt IS NULL AND sh.deletedAt IS NULL
                )
            )
            FROM Host h
            WHERE h.nickname LIKE CONCAT('%', :name, '%') ESCAPE '\\'
            """,
        countQuery = "SELECT COUNT(h) FROM Host h WHERE h.nickname LIKE CONCAT('%', :name, '%') ESCAPE '\\'"
    )
    Page<HostDetailResponse> findByNameContaining(
        @Param("name") String name,
        Pageable pageable
    );

    /**
     * [from, to) 범위에 가입한 호스트 수를 KST 날짜별로 집계한다. 탈퇴 여부와 무관하게 모두 포함한다.
     * offsetMinutes는 created_at(JVM 기본 타임존)을 KST로 옮기기 위한 분 단위 오프셋이다.
     */
    @Query(
        nativeQuery = true,
        value = """
            SELECT DATE_FORMAT(DATE_ADD(h.created_at, INTERVAL :offsetMinutes MINUTE), '%Y-%m-%d') AS day,
                   COUNT(*) AS count
            FROM host h
            WHERE h.created_at >= :from AND h.created_at < :to
            GROUP BY day
            """
    )
    List<DailyCountRow> countDailySignups(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("offsetMinutes") int offsetMinutes
    );

    default Host getByIdOrThrow(Long id) {
        if (id == null) {
            throw new BaseNullPointerException("호스트의 id는 null일 수 없습니다. id: " + id);
        }
        return findById(id)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 호스트입니다. id: " + id));
    }
}
