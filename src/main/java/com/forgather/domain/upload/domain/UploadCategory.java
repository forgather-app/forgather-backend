package com.forgather.domain.upload.domain;

public enum UploadCategory {
    PRODUCT,
    GUESTBOOK,
    SPACE,
    EXHIBITION,
    ;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
