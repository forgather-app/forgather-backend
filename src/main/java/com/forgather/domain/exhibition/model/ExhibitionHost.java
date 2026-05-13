package com.forgather.domain.exhibition.model;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ExhibitionHost extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Column(name = "is_creator", nullable = false)
    private boolean isCreator;

    public ExhibitionHost(Exhibition exhibition, Host host, boolean isCreator) {
        validateRequiredFields(exhibition, host);
        this.exhibition = exhibition;
        this.host = host;
        this.isCreator = isCreator;
    }

    private void validateRequiredFields(Exhibition exhibition, Host host) {
        if (exhibition == null) {
            throw new BaseNullPointerException("전시는 null일 수 없습니다.");
        }
        if (host == null) {
            throw new BaseNullPointerException("전시 호스트는 null일 수 없습니다.");
        }
    }
}
