package com.project.authservice.service;

import com.project.authservice.dto.AccessProfileDto;

import java.util.List;

public interface AccessProfileService {
    List<AccessProfileDto> getAllProfiles();
    AccessProfileDto updatePermissions(Long id, AccessProfileDto request);
}
