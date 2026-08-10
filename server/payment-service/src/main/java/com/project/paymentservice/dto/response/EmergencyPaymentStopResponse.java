package com.project.paymentservice.dto.response;

import java.util.List;

public record EmergencyPaymentStopResponse(
        int stoppedPaymentAttemptCount,
        List<String> alreadySuccessfulBookingPublicIds
) {
}
