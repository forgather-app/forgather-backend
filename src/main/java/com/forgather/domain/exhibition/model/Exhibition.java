package com.forgather.domain.exhibition.model;

import java.time.LocalDate;

import com.forgather.domain.model.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Exhibition extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "description")
    private String description;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "operation_type")
    private OperationType operationType;

    @Column(name = "operation_notice")
    private String operationNotice;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "location_type")
    private LocationType locationType;

    @Column(name = "online_url")
    private String onlineUrl;

    @Column(name = "base_address")
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;
}
