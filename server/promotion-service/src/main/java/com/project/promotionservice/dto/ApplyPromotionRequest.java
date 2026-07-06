package com.project.promotionservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyPromotionRequest {

    @NotBlank(message = "Promotion code must not be blank")
    @Size(max = 50, message = "Promotion code must not exceed 50 characters")
    private String promotionCode;

    @NotNull(message = "Booking ID must not be null")
    @Positive(message = "Booking ID must be greater than 0")
    private Long bookingId;

    @NotNull(message = "User ID must not be null")
    @Positive(message = "User ID must be greater than 0")
    private Long userId;

    @NotNull(message = "Booking amount must not be null")
    @DecimalMin(value = "0.0", message = "Booking amount must be greater than or equal to 0")
    private BigDecimal bookingAmount;

    @NotNull(message = "Booking expires at must not be null")
    private LocalDateTime bookingExpiresAt;
}
