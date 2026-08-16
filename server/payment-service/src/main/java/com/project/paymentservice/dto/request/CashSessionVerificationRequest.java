package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CashSessionVerificationRequest(
        @NotBlank @Size(min = 5, max = 1000) String note) {
}
