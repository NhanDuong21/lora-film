package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmUsageRequest {

    @NotNull(message = "Booking ID must not be null")
    @Positive(message = "Booking ID must be greater than 0")
    private Long bookingId;

    private LocalDateTime confirmedAt;
}
