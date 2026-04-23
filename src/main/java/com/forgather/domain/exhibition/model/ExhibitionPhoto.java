package com.forgather.domain.exhibition.model;

import com.forgather.domain.model.Photo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ExhibitionPhoto extends Photo {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;
}
