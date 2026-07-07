package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevertUsageRequest {

    @NotNull(message = "Booking ID must not be null")
    @Positive(message = "Booking ID must be greater than 0")
    private Long bookingId;

    @NotBlank(message = "Revert reason must not be blank")
    @Size(max = 255, message = "Revert reason must not exceed 255 characters")
    private String reason;
}
