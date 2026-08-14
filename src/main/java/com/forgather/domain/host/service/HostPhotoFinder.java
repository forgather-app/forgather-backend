package com.forgather.domain.host.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.model.Photo;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostPhotoFinder {

    private final HostProfilePhotoRepository hostProfilePhotoRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final SpacePhotoRepository spacePhotoRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final GuestBookCardPhotoRepository guestBookCardPhotoRepository;

    /**
     * 호스트가 남긴 저장소 사진을 soft delete된 행까지 포함해 모두 수집한다.
     */
    @Transactional(readOnly = true)
    public List<Photo> findAllIncludingDeleted(Host host) {
        List<Photo> photos = new ArrayList<>();
        photos.addAll(hostProfilePhotoRepository.findAllByHost(host));
        photos.addAll(findAllSpacePhotos(host));
        // SpacePhoto.empty()처럼 path가 빈 행은 저장소 객체가 없어 파기 대상에서 제외한다
        return photos.stream()
            .filter(photo -> photo.getPath() != null && !photo.getPath().isBlank())
            .toList();
    }

    private List<Photo> findAllSpacePhotos(Host host) {
        List<Space> spaces = spaceHostRepository.findAllSpacesByHost(host);
        if (spaces.isEmpty()) {
            return List.of();
        }
        List<Photo> photos = new ArrayList<>();
        photos.addAll(spacePhotoRepository.findAllBySpaceIn(spaces));
        photos.addAll(productPhotoRepository.findAllBySpaceIn(spaces));
        photos.addAll(guestBookCardPhotoRepository.findAllBySpaceIn(spaces));
        return photos;
    }
}
