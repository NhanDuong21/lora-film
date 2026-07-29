package com.project.authservice.service;

public interface RegistrationIntegrationService {
    /**
     * Receives extra profile fields from registration flow and forwards them to other services.
     * Implementations can call downstream profile services, message buses, etc.
     *
     * @param accountId created account id
     * @param fullName user's full name
     * @param citizenId user's citizen id
     * @param gender user's gender
     * @param dob user's date of birth in ISO format (yyyy-MM-dd)
     */
    void forwardProfileData(Long accountId, String fullName, String citizenId, String gender, String dob);
}
