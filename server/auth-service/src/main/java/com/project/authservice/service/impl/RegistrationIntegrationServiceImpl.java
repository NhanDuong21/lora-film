package com.project.authservice.service.impl;

import org.springframework.stereotype.Service;

import com.project.authservice.service.RegistrationIntegrationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegistrationIntegrationServiceImpl implements RegistrationIntegrationService {

    @Override
    public void forwardProfileData(Long accountId, String fullName, String citizenId, String gender, String dob) {
        // Placeholder implementation: currently just log the received data.
        // Replace this with actual calls to profile service, message broker, etc.
        log.info("Forwarding profile data for accountId={}: fullName='{}', citizenId='{}', gender='{}', dob='{}'",
                accountId, fullName, citizenId, gender, dob);
    }
}
