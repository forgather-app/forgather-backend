package com.forgather.back_office.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.forgather.back_office.annotation.Admin;
import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.service.AdminHostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/hosts")
@RequiredArgsConstructor
public class AdminHostController {

    private final AdminHostService adminHostService;

    @GetMapping
    public ResponseEntity<AdminHostResponse> getAllHosts(
        @PageableDefault(size = 15, sort = {"createdAt"}, direction = Sort.Direction.DESC)
        Pageable pageable,
        @Admin AdminUser adminUser
    ) {
        var response = adminHostService.getAllHosts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{hostId}/spaces")
    public ResponseEntity<HostSpacesResponse> getHostSpaces(
        @PathVariable(name = "hostId") Long hostId,
        @Admin AdminUser adminUser
    ) {
        var response = adminHostService.getHostSpaces(hostId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/by-name")
    public ResponseEntity<AdminHostResponse> getHostsByName(
        @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false, defaultValue = "") String name,
        @Admin AdminUser adminUser
    ) {
        var response = adminHostService.searchHostsByName(name, pageable);
        return ResponseEntity.ok(response);
    }
}
