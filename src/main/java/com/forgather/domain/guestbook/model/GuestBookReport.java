package com.forgather.domain.guestbook.model;

import java.time.LocalDateTime;

import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

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
    private AppUser hostUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private AppUser reporterUser;

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

    public GuestBookReport(
        GuestBookCard guestBookCard,
        AppUser hostUser,
        AppUser reporterUser,
        ReporterType reporterType,
        GuestBookReportReason reason,
        String detail
    ) {
        validateRequiredFields(guestBookCard, hostUser, reporterUser, reporterType, reason);
        validateDetail(detail);
        this.guestBookCard = guestBookCard;
        this.hostUser = hostUser;
        this.reporterUser = reporterUser;
        this.reason = reason;
        this.reporterType = reporterType;
        this.detail = detail;
        this.nicknameSnapshot = guestBookCard.getNickname();
        this.messageSnapshot = guestBookCard.getMessage();
        this.createdAtSnapshot = guestBookCard.getCreatedAt();
    }

    private void validateRequiredFields(
        GuestBookCard guestBookCard,
        AppUser hostUser,
        AppUser reporterUser,
        ReporterType reporterType,
        GuestBookReportReason reason
    ) {
        if (guestBookCard == null) {
            throw new BaseNullPointerException("방명록 카드는 null일 수 없습니다.");
        }
        if (hostUser == null) {
            throw new BaseNullPointerException("호스트 유저는 null일 수 없습니다.");
        }
        if (reporterUser == null) {
            throw new BaseNullPointerException("신고자는 null일 수 없습니다.");
        }
        if (reporterType == null) {
            throw new BaseNullPointerException("신고자 유형은 null일 수 없습니다.");
        }
        if (reason == null) {
            throw new BaseNullPointerException("신고 사유는 null일 수 없습니다.");
        }
    }

    private void validateDetail(String detail) {
        if (detail == null) {
            return;
        }
        int length = TextLengthCounter.count(detail);
        if (length > 200 || length < 5) {
            throw new BaseException("상세 사유는 최소 5자, 최대 200자까지 입력 가능합니다.");
        }
    }
}
