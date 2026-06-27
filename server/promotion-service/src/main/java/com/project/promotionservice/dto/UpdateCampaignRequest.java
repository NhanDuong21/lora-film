package com.project.promotionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignRequest {

    @NotBlank(message = "campaignName is required")
    @Size(max = 150, message = "campaignName cannot exceed 150 characters")
    private String campaignName;

    private String description;

    @NotNull(message = "startDate is required")
    private LocalDateTime startDate;

    @NotNull(message = "endDate is required")
    private LocalDateTime endDate;

    @Builder.Default
    private Boolean isActive = true;
}
