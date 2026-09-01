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
import com.forgather.domain.exhibition.model.Location;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;
import com.forgather.domain.exhibition.repository.ExhibitionPhotoRepository;
import com.forgather.domain.exhibition.repository.ExhibitionRepository;
import com.forgather.domain.exhibition.repository.ExhibitionTimeRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.FilePathGenerator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionHostRepository exhibitionHostRepository;
    private final ExhibitionPhotoRepository exhibitionPhotoRepository;
    private final ExhibitionTimeRepository exhibitionTimeRepository;
    private final ContentsStorage contentsStorage;

    @Transactional
    public ExhibitionResponse create(Host host, CreateExhibitionRequest request) {
        Exhibition exhibition = exhibitionRepository.save(buildExhibition(request));

        ExhibitionPhoto photo = savePhotoIfPresent(request, exhibition);

        ExhibitionTimes exhibitionTimes = buildExhibitionTimes(request, exhibition);
        exhibitionTimeRepository.saveAll(exhibitionTimes.getValues());

        exhibitionHostRepository.save(new ExhibitionHost(exhibition, host, true));

        return ExhibitionResponse.of(exhibition, photo, exhibitionTimes, host);
    }

    private ExhibitionPhoto savePhotoIfPresent(CreateExhibitionRequest request, Exhibition exhibition) {
        if (request.photo() == null) {
            return null;
        }
        String photoPath = buildPhotoPath(request.photo().uploadFileName());
        return exhibitionPhotoRepository.save(request.photo().toEntity(photoPath, exhibition));
    }

    private String buildPhotoPath(String uploadFileName) {
        return FilePathGenerator.generateExhibitionContentsFilePath(
            contentsStorage.getRootDirectory(), uploadFileName
        );
    }

    private Exhibition buildExhibition(CreateExhibitionRequest request) {
        return new Exhibition(
            request.title(), request.startDate(), request.endDate(),
            request.description(), request.operationNotice(),
            buildLocation(request.location())
        );
    }

    private Location buildLocation(LocationRequest request) {
        if (request == null) {
            return null;
        }
        return new Location(request.locationType(), request.url(), request.baseAddress(), request.detailAddress());
    }

    private ExhibitionTimes buildExhibitionTimes(CreateExhibitionRequest request, Exhibition exhibition) {
        List<ExhibitionTime> exhibitionTimes = request.operatingHours().stream()
            .map(req -> new ExhibitionTime(exhibition, req.dayOfWeek(), req.startTime(), req.endTime()))
            .toList();
        return new ExhibitionTimes(exhibitionTimes);
    }
}
