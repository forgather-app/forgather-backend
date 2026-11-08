package com.forgather.domain.export.dto;

import java.util.List;

public record SpaceArchive(
    String spaceName,
    String nickname,
    List<GuestbookInfo> guestbookInfos
) {

    public record GuestbookInfo(
        String author,
        String message,
        String createdAt,
        List<String> photoPaths
    ) {
    }
}
