package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.*;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.service.PromotionAvailabilityService;
import com.project.promotionservice.service.PromotionCampaignAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PromotionCampaignAdminServiceImpl implements PromotionCampaignAdminService {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionAvailabilityService availabilityService;

    public PromotionCampaignAdminServiceImpl(PromotionCampaignRepository campaignRepository,
                                             PromotionAvailabilityService availabilityService) {
        this.campaignRepository = campaignRepository;
        this.availabilityService = availabilityService;
    }

    private String getCurrentActorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "anonymous";
        }
        return authentication.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();
        
        try {
            // Trim campaign name
            String campaignName = request.getCampaignName() != null ? request.getCampaignName().trim() : null;
            if (campaignName == null || campaignName.isEmpty()) {
                throw new BusinessException("campaignName is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }

            // Validate date range
            if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
                throw new BusinessException("endDate must be after startDate", "CAMPAIGN_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
            }

            // Create campaign entity
            PromotionCampaign campaign = PromotionCampaign.builder()
                    .campaignName(campaignName)
                    .description(request.getDescription())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .active(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            PromotionCampaign savedCampaign = campaignRepository.save(campaign);
            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getCampaignStatus(savedCampaign, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: CREATE_CAMPAIGN, CampaignId: {}, Duration: {}ms",
                    actorId, savedCampaign.getId(), duration);

            return CampaignResponse.builder()
                    .campaignId(savedCampaign.getId())
                    .campaignName(savedCampaign.getCampaignName())
                    .description(savedCampaign.getDescription())
                    .startDate(savedCampaign.getStartDate())
                    .endDate(savedCampaign.getEndDate())
                    .isActive(savedCampaign.isActive())
                    .availabilityStatus(availability)
                    .createdAt(savedCampaign.getCreatedAt())
                    .updatedAt(savedCampaign.getUpdatedAt())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: CREATE_CAMPAIGN, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: CREATE_CAMPAIGN, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignPageResponse getCampaigns(Boolean isActive, String availabilityStatus, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // To be implemented in Unit 4
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id) {
        // To be implemented in Unit 4
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {
        // To be implemented in Unit 5
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampaignResponse updateCampaignStatus(Long id, UpdateCampaignStatusRequest request) {
        // To be implemented in Unit 6
        return null;
    }
}
