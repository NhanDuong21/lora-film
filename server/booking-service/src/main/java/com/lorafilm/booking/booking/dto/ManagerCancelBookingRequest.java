package com.lorafilm.booking.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManagerCancelBookingRequest(
        @NotBlank @Size(min = 5, max = 500) String reason) {
}
