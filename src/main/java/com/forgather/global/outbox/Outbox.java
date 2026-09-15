package com.forgather.global.outbox;

import com.forgather.domain.model.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private OutboxType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    public Outbox(OutboxType type, OutboxStatus status, String payload, int failCount) {
        this.type = type;
        this.status = status;
        this.payload = payload;
        this.failCount = failCount;
    }

    public static Outbox pending(OutboxType type, String payload) {
        return new Outbox(type, OutboxStatus.PENDING, payload, 0);
    }

    public void complete() {
        payload = null;
        status = OutboxStatus.COMPLETED;
    }

    public void fail() {
        failCount++;
        status = OutboxStatus.FAILED;
    }
}
