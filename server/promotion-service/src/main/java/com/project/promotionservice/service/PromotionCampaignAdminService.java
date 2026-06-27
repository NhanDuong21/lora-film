package com.project.promotionservice.service;

import com.project.promotionservice.dto.*;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface PromotionCampaignAdminService {
    CampaignResponse createCampaign(CreateCampaignRequest request);
    CampaignPageResponse getCampaigns(Boolean isActive, String availabilityStatus, LocalDateTime from, LocalDateTime to, Pageable pageable);
    CampaignResponse getCampaignById(Long id);
    CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request);
    CampaignResponse updateCampaignStatus(Long id, UpdateCampaignStatusRequest request);
}
