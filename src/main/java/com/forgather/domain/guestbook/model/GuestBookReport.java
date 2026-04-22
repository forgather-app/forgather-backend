package com.forgather.domain.guestbook.model;

import java.time.LocalDateTime;

import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.auth.model.Host;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class GuestBookReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_book_card_id", nullable = false)
    private GuestBookCard guestBookCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private Host hostUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private Host reporterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", nullable = false)
    private GuestBookReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false)
    private ReporterType reporterType;  // HOST

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "nickname_snapshot", length = 10, nullable = false)
    private String nicknameSnapshot;

    @Column(name = "message_snapshot", length = 500, nullable = false)
    private String messageSnapshot;

    @Column(name = "created_at_snapshot", nullable = false)
    private LocalDateTime createdAtSnapshot;
}
