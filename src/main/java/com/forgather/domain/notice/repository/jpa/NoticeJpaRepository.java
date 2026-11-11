package com.forgather.domain.notice.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.notice.model.Notice;
import com.forgather.domain.notice.repository.NoticeRepository;

public interface NoticeJpaRepository extends JpaRepository<Notice, Long>, NoticeRepository {
}
