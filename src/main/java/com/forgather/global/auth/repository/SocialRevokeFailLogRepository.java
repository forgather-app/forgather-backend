package com.forgather.global.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.global.auth.model.SocialRevokeFailLog;

public interface SocialRevokeFailLogRepository extends JpaRepository<SocialRevokeFailLog, Long> {

    List<SocialRevokeFailLog> findAllByCompletedAtIsNull();
}
