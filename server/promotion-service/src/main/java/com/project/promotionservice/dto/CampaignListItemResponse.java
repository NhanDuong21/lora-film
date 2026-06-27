package com.project.promotionservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignListItemResponse {
    private Long campaignId;
    private String campaignName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
