package com.project.paymentservice.dto.response;

import java.util.List;

/** Non-mutating payment snapshot used before a break-glass promotion release. */
public record EmergencyPaymentAssessmentResponse(
        List<String> activePaymentBookingPublicIds,
        List<String> successfulPaymentBookingPublicIds) {
}
