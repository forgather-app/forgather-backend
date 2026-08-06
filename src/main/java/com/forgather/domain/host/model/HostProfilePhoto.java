package com.forgather.domain.host.model;

import com.forgather.domain.model.Photo;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostProfilePhoto extends Photo {

    private static final String EMPTY_ORIGINAL_NAME = "";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    public HostProfilePhoto(String path, Long capacity, Host host) {
        super(EMPTY_ORIGINAL_NAME, path, capacity);
        validateRequiredFields(host);
        this.host = host;
    }

    private void validateRequiredFields(Host host) {
        if (host == null) {
            throw new BaseNullPointerException("호스트는 null일 수 없습니다.");
        }
    }
}
