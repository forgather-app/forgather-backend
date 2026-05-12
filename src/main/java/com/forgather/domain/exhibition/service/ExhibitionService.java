package com.forgather.domain.exhibition.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.dto.LocationRequest;
import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.exhibition.model.ExhibitionPhoto;
import com.forgather.domain.exhibition.model.ExhibitionTime;
import com.forgather.domain.exhibition.model.ExhibitionTimes;
import com.forgather.domain.exhibition.model.LocationType;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;
import com.forgather.domain.exhibition.repository.ExhibitionPhotoRepository;
import com.forgather.domain.exhibition.repository.ExhibitionRepository;
import com.forgather.domain.exhibition.repository.ExhibitionTimeRepository;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionHostRepository exhibitionHostRepository;
    private final ExhibitionPhotoRepository exhibitionPhotoRepository;
    private final ExhibitionTimeRepository exhibitionTimeRepository;

    @Transactional
    public ExhibitionResponse create(Host host, CreateExhibitionRequest request) {
        Exhibition exhibition = exhibitionRepository.save(buildExhibition(request));

        // TODO: photo path 검증(정말 s3에 존재하는 객체인가?)
        ExhibitionPhoto photo = exhibitionPhotoRepository.save(
            new ExhibitionPhoto(request.imagePath(), request.imageCapacity(), exhibition)
        );

        ExhibitionTimes exhibitionTimes = new ExhibitionTimes(buildExhibitionTimes(request, exhibition));
        exhibitionTimeRepository.saveAll(exhibitionTimes.getValues());

        exhibitionHostRepository.save(new ExhibitionHost(exhibition, host, true));

        return ExhibitionResponse.of(exhibition, photo, exhibitionTimes, host);
    }

    private Exhibition buildExhibition(CreateExhibitionRequest request) {
        LocationRequest location = request.location();
        LocationType locationType = location != null ? location.locationType() : null;
        String url = location != null ? location.url() : null;
        String baseAddress = location != null ? location.baseAddress() : null;
        String detailAddress = location != null ? location.detailAddress() : null;
        return new Exhibition(
            request.title(), request.startDate(), request.endDate(),
            request.description(), request.operationNotice(),
            locationType, url, baseAddress, detailAddress
        );
    }

    private List<ExhibitionTime> buildExhibitionTimes(CreateExhibitionRequest request, Exhibition exhibition) {
        return request.operatingHours().stream()
            .map(req -> new ExhibitionTime(exhibition, req.dayOfWeek(), req.startTime(), req.endTime()))
            .toList();
    }
}
