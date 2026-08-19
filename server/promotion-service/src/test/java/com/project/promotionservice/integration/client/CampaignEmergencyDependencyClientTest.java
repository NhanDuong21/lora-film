package com.project.promotionservice.integration.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignEmergencyDependencyClientTest {

    @Test
    void missingBookingOrPaymentAssessmentTokenFailsStartupValidation() {
        assertThrows(IllegalStateException.class,
                () -> new CampaignEmergencyDependencyClient(
                        RestClient.builder(), "http://localhost:8083", "",
                        "http://localhost:8084", "payment-token"));
        assertThrows(IllegalStateException.class,
                () -> new CampaignEmergencyDependencyClient(
                        RestClient.builder(), "http://localhost:8083", "booking-token",
                        "http://localhost:8084", ""));
    }
}
