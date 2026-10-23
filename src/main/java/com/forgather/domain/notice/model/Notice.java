package com.forgather.domain.notice.model;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    public Notice(String title, String body) {
        validateText(title, "공지 제목", 100);
        validateText(body, "공지 본문", 10000);
        this.title = title;
        this.body = body;
    }

    private void validateText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BaseException(field + "은 필수입니다.");
        }
        int length = TextLengthCounter.count(value);
        if (length > maxLength) {
            throw new BaseException("%s은 최대 %d자까지 입력 가능합니다. length: %d"
                .formatted(field, maxLength, length));
        }
    }
}
