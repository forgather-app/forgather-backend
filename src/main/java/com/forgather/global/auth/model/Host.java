package com.forgather.global.auth.model;

import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Host extends BaseTimeEntity {

    private static final int MAX_NICKNAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "nickname", length = MAX_NICKNAME_LENGTH)
    private String nickname;

    @Column(name = "picture_url")
    private String pictureUrl;

    public Host(String name, String pictureUrl) {
        validateName(name);
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    private void validateName(String name) {
        if (name == null) {
            throw new BaseNullPointerException("이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (name.isBlank()) {
            throw new BaseException("이름은 공백만 입력할 수 없습니다.");
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null) {
            throw new BaseNullPointerException("닉네임은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (nickname.isBlank()) {
            throw new BaseException("닉네임은 공백만 입력할 수 없습니다.");
        }
        int length = TextLengthCounter.count(nickname);
        if (length > MAX_NICKNAME_LENGTH) {
            throw new BaseException("닉네임은 최대 20자까지 입력 가능합니다. nickname.length: " + length);
        }
    }

    public boolean hasValidNickname() {
        return nickname != null
            && !nickname.isBlank()
            && TextLengthCounter.count(nickname) <= MAX_NICKNAME_LENGTH;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Host))
            return false;
        Host host = (Host)o;
        return id != null && Objects.equals(id, host.id);
    }

    @Override
    public int hashCode() {
        return Host.class.hashCode();
    }
}
