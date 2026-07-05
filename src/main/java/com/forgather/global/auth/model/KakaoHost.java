package com.forgather.global.auth.model;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "host_kakao")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoHost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    public KakaoHost(Host host, String userId, String name) {
        validateName(name);
        this.host = host;
        this.userId = userId;
        this.name = name;
    }

    private void validateName(String name) {
        if (name == null) {
            throw new BaseNullPointerException("카카오 닉네임은 null일 수 없습니다.", 400);
        }
        if (name.isBlank()) {
            throw new BaseException("카카오 닉네임은 공백만 입력할 수 없습니다.");
        }
    }
}
