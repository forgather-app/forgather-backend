package com.forgather.domain.guestbook.model;

import com.forgather.domain.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class GuestBookReportReason extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true, nullable = false)
    private String code;

    @Column(length = 30, nullable = false)
    private String label;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean isActive;
}
