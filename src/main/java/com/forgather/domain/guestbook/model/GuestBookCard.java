package com.forgather.domain.guestbook.model;

import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_ADMIN;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_HOST;
import static com.forgather.domain.guestbook.model.VisibilityStatus.VISIBLE;

import org.springframework.http.HttpStatus;

import com.forgather.domain.guestbook.exception.GuestbookCardNotReadableException;
import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.domain.space.model.Space;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;
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
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestBookCard extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "nickname", length = 10)
    private String nickname;

    @Column(name = "message", length = 500, nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false)
    private VisibilityStatus visibilityStatus = VISIBLE;

    public GuestBookCard(Space space, String nickname, String message) {
        validateRequiredFields(space, nickname, message);
        validateNickname(nickname);
        validateMessage(message);
        this.space = space;
        this.nickname = nickname;
        this.message = message;
        this.isRead = false;
        this.visibilityStatus = VISIBLE;
    }

    private void validateRequiredFields(Space space, String nickname, String message) {
        if (space == null) {
            throw new BaseNullPointerException("방명록 카드 스페이스는 null일 수 없습니다.");
        }
        if (nickname == null) {
            throw new BaseNullPointerException("방문자 닉네임은 null일 수 없습니다.");
        }
        if (message == null) {
            throw new BaseNullPointerException("방명록 카드 메세지는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateNickname(String nickname) {
        if (nickname.isBlank()) {
            throw new BaseException("방문자 닉네임은 공백만 입력할 수 없습니다.");
        }
        int length = TextLengthCounter.count(nickname);
        if (length > 10) {
            throw new BaseException("방문자 닉네임은 최대 10자까지 입력 가능합니다. nickname.length: " + length);
        }
    }

    private void validateMessage(String message) {
        int length = TextLengthCounter.count(message);
        if (length > 400) {
            throw new BaseException("방명록 카드 메세지는 최대 400까지 입력 가능합니다. message.length: " + length);
        }
    }

    public boolean equalsSpace(Space other) {
        return space.equals(other);
    }

    public void read(boolean isSpaceHost) {
        validateCanReadByVisibilityStatus(isSpaceHost);
        if (isSpaceHost) {
            isRead = true;
        }
    }

    private void validateCanReadByVisibilityStatus(boolean isSpaceHost) {
        if (visibilityStatus == HIDDEN_BY_ADMIN ||
            visibilityStatus == HIDDEN_BY_HOST && !isSpaceHost
        ) {
            throw new GuestbookCardNotReadableException();
        }
    }

    public void hideByAdmin() {
        visibilityStatus = HIDDEN_BY_ADMIN;
    }
}
