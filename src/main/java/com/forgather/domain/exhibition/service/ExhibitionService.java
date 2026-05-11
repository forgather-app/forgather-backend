package com.forgather.domain.exhibition.service;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.dto.OperatingHourRequest;
import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.exhibition.model.ExhibitionPhoto;
import com.forgather.domain.exhibition.model.ExhibitionTime;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;
import com.forgather.domain.exhibition.repository.ExhibitionPhotoRepository;
import com.forgather.domain.exhibition.repository.ExhibitionRepository;
import com.forgather.domain.exhibition.repository.ExhibitionTimeRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseException;

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
        validateNoDuplicateDayOfWeek(request.operatingHours());

        Exhibition exhibition = exhibitionRepository.save(new Exhibition(
            request.title(), request.startDate(), request.endDate(),
            request.description(), request.operationNotice(),
            request.location().locationType(), request.location().url(),
            request.location().baseAddress(), request.location().detailAddress()
        ));

        // TODO: photo path 검증(정말 s3에 존재하는 객체인가?)
        ExhibitionPhoto photo = exhibitionPhotoRepository.save(
            new ExhibitionPhoto(request.imagePath(), request.imageCapacity(), exhibition)
        );

        List<ExhibitionTime> times = List.of();
        if (request.operatingHours() != null) {
            times = request.operatingHours().stream()
                .map(req -> new ExhibitionTime(exhibition, req.dayOfWeek(), req.startTime(), req.endTime()))
                .map(exhibitionTimeRepository::save)
                .toList();
        }

        exhibitionHostRepository.save(new ExhibitionHost(exhibition, host, true));

        return ExhibitionResponse.of(exhibition, photo, times, host);
    }

    private void validateNoDuplicateDayOfWeek(List<OperatingHourRequest> operatingHours) {
        if (operatingHours == null || operatingHours.isEmpty()) {
            return;
        }
        Set<DayOfWeek> seen = new HashSet<>();
        for (OperatingHourRequest hour : operatingHours) {
            if (!seen.add(hour.dayOfWeek())) {
                throw new BaseException("같은 요일은 중복될 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }
    }
}
