package com.project.authservice.service.impl;

import org.springframework.stereotype.Service;

import com.project.authservice.service.RegistrationIntegrationService;


@Service
public class RegistrationIntegrationServiceImpl implements RegistrationIntegrationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationIntegrationServiceImpl.class);

    @Override
    public void forwardProfileData(Long accountId, String fullName, String citizenId, String gender, String dob) {
        // Placeholder implementation: currently just log the received data.
        // Replace this with actual calls to profile service, message broker, etc.
        log.info("Forwarding profile data for accountId={}: fullName='{}', citizenId='{}', gender='{}', dob='{}'",
                accountId, fullName, citizenId, gender, dob);
    }
}
