package com.forgather.domain.term.model;

import java.time.LocalDateTime;

import com.forgather.global.exception.BaseNullPointerException;

import lombok.Getter;

/**
 * 타입별 최신 약관과 호스트의 마지막 동의 이력을 묶어 동의 현황을 판정한다.
 */
public class TermAgreement {

    @Getter
    private final Term latestTerm;

    private final HostTermHistory lastHistory;

    /**
     * @param latestTerm  타입별 최신 약관
     * @param lastHistory 해당 타입의 마지막 이력. 이력이 없으면 null
     */
    public TermAgreement(Term latestTerm, HostTermHistory lastHistory) {
        validateRequiredFields(latestTerm);
        this.latestTerm = latestTerm;
        this.lastHistory = lastHistory;
    }

    private void validateRequiredFields(Term latestTerm) {
        if (latestTerm == null) {
            throw new BaseNullPointerException("동의 현황의 약관은 null일 수 없습니다.");
        }
    }

    public boolean isAgreed() {
        return isAgreeAction()
            && latestTerm.isAgreedVersionValid(lastHistory.getTerm().getVersion());
    }

    public LocalDateTime getAgreedAt() {
        if (!isAgreed()) {
            return null;
        }
        return lastHistory.getCreatedAt();
    }

    /**
     * 선택 약관은 거절·철회·이력 없음을 "사용자가 원치 않은 것"으로 보고 다시 묻지 않는다.
     * 동의 의사가 있었는데 개정으로 무효화된 경우에만 재동의를 권유한다.
     */
    public boolean isReagreementRequired() {
        if (isAgreed()) {
            return false;
        }
        if (latestTerm.getType().isRequired()) {
            return true;
        }
        return isAgreeAction();
    }

    private boolean isAgreeAction() {
        return lastHistory != null && lastHistory.isAgreeAction();
    }
}
