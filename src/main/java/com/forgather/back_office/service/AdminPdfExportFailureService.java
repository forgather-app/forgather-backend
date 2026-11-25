package com.forgather.back_office.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminPdfExportFailureDetailResponse;
import com.forgather.back_office.dto.AdminPdfExportFailureResponse;
import com.forgather.back_office.repository.AdminPdfExportFailureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPdfExportFailureService {

    private final AdminPdfExportFailureRepository repository;

    public AdminPdfExportFailureResponse getFailures(Pageable pageable) {
        return AdminPdfExportFailureResponse.from(repository.findAllByDeletedAtIsNull(pageable));
    }

    public AdminPdfExportFailureDetailResponse getFailure(Long id) {
        return AdminPdfExportFailureDetailResponse.from(repository.getByIdAndDeletedAtIsNullOrThrow(id));
    }
}
