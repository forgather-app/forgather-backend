package com.forgather.domain.space.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.service.ContentsStorage;
import com.forgather.domain.space.service.PhotoService;
import com.forgather.domain.space.service.SpaceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContentsStorageScheduler {

    private final SpaceService spaceService;
    private final PhotoService photoService;
    private final ContentsStorage contentsStorage;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredContents() {
        List<Space> expiredSpaces = spaceService.getExpiredSpaces();
        if (expiredSpaces.isEmpty()) {
            return;
        }

        for (Space expiredSpace : expiredSpaces) {
            List<String> paths = photoService.getPathsBySpace(expiredSpace);
            photoService.deleteAllInSpace(expiredSpace);
            contentsStorage.deleteContents(paths);
        }
    }

    // TODO: 활성화된 스페이스들의 고아 객체 제거
}
