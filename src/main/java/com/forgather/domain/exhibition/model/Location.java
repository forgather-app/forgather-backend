package com.forgather.domain.exhibition.model;

import com.forgather.global.exception.BaseException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Location {

    @Enumerated(value = EnumType.STRING)
    @Column(name = "location_type")
    private LocationType type;

    @Column(name = "online_url")
    private String onlineUrl;

    @Column(name = "base_address")
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    public Location(LocationType type, String onlineUrl, String baseAddress, String detailAddress) {
        validate(type, onlineUrl, baseAddress);
        this.type = type;
        this.onlineUrl = onlineUrl;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    private void validate(LocationType type, String onlineUrl, String baseAddress) {
        if (type == null) {
            throw new BaseException("장소 유형은 필수입니다.");
        }
        if (type == LocationType.ONLINE && onlineUrl == null) {
            throw new BaseException("온라인 전시의 경우 온라인 URL은 필수입니다.");
        }
        if (type == LocationType.OFFLINE && baseAddress == null) {
            throw new BaseException("오프라인 전시의 경우 기본 주소는 필수입니다.");
        }
    }
}
