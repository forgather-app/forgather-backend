package com.forgather.domain.term.model;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseNullPointerException;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "host_term_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostTermHistory extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private HostTermHistoryAction action;

    public HostTermHistory(Host host, Term term, HostTermHistoryAction action) {
        validateRequiredFields(host, term, action);
        this.host = host;
        this.term = term;
        this.action = action;
    }

    private void validateRequiredFields(Host host, Term term, HostTermHistoryAction action) {
        if (host == null) {
            throw new BaseNullPointerException("약관 동의 이력의 호스트는 null일 수 없습니다.");
        }
        if (term == null) {
            throw new BaseNullPointerException("약관 동의 이력의 약관은 null일 수 없습니다.");
        }
        if (action == null) {
            throw new BaseNullPointerException("약관 동의 이력의 액션은 null일 수 없습니다.");
        }
    }
}
