package com.forgather.domain.host.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.host.model.SocialRevokeFailLog;

public interface SocialRevokeFailLogRepository extends JpaRepository<SocialRevokeFailLog, Long> {

    List<SocialRevokeFailLog> findAllByCompletedAtIsNull();
}
