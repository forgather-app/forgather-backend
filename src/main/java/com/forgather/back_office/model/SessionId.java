package com.forgather.back_office.model;

import java.util.Objects;

import com.forgather.global.util.RandomCodeGenerator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SessionId {

    private static final int SESSION_ID_LENGTH = 10;

    private final String value;

    public static SessionId generate(RandomCodeGenerator randomCodeGenerator) {
        return new SessionId(randomCodeGenerator.generate(SESSION_ID_LENGTH));
    }

    public static SessionId from(String value) {
        return new SessionId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SessionId sessionId))
            return false;
        return Objects.equals(getValue(), sessionId.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}
