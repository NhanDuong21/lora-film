package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
