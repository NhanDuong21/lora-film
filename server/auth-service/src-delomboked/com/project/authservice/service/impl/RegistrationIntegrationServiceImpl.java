package com.project.authservice.service.impl;

import org.springframework.stereotype.Service;

import com.project.authservice.service.RegistrationIntegrationService;
import com.project.authservice.service.AuthOutboxService;

import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class RegistrationIntegrationServiceImpl implements RegistrationIntegrationService {
    private final AuthOutboxService outboxService;

    public RegistrationIntegrationServiceImpl(AuthOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public void forwardProfileData(Long accountId, String fullName, String citizenId, String gender, String dob) {
        outboxService.record("PROFILE_DATA_FORWARD_REQUESTED", accountId, Map.of(
                "accountId", accountId,
                "fullName", fullName,
                "citizenId", citizenId,
                "gender", gender,
                "dateOfBirth", dob));
    }
}
