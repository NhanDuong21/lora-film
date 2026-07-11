package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePromotionStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
