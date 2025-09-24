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

    /**
     * 만료된 스페이스의 컨텐츠를 삭제합니다.
     */
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

    /**
     * 활성화된 스페이스의 DB에 존재하지 않는 컨텐츠를 삭제합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOrphanContents() {
        List<Space> activatedSpaces = spaceService.getActivatedSpaces();
        if (activatedSpaces.isEmpty()) {
            return;
        }

        for (Space activatedSpace : activatedSpaces) {
            List<String> paths = photoService.getPathsBySpace(activatedSpace);
            contentsStorage.deleteOrphanContents(activatedSpace.getCode(), paths);
        }
    }
}
