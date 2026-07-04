package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionValidationRequest {

    @NotBlank(message = "Promotion code must not be blank")
    @Size(max = 50, message = "Promotion code must not exceed 50 characters")
    private String promotionCode;

    @NotNull(message = "Booking ID must not be null")
    private Long bookingId;
}
