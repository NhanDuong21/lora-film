package com.project.promotionservice.service.impl;

import com.project.promotionservice.dto.*;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.service.PromotionAvailabilityService;
import com.project.promotionservice.service.PromotionCampaignAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PromotionCampaignAdminServiceImpl implements PromotionCampaignAdminService {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionAvailabilityService availabilityService;

    public PromotionCampaignAdminServiceImpl(PromotionCampaignRepository campaignRepository,
                                             PromotionRepository promotionRepository,
                                             PromotionAvailabilityService availabilityService) {
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
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
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            LocalDateTime now = LocalDateTime.now();
            Specification<PromotionCampaign> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (isActive != null) {
                    predicates.add(cb.equal(root.get("active"), isActive));
                }

                if (availabilityStatus != null) {
                    switch (availabilityStatus.toUpperCase()) {
                        case "DISABLED":
                            predicates.add(cb.equal(root.get("active"), false));
                            break;
                        case "UPCOMING":
                            predicates.add(cb.equal(root.get("active"), true));
                            predicates.add(cb.greaterThan(root.get("startDate"), now));
                            break;
                        case "ACTIVE":
                            predicates.add(cb.equal(root.get("active"), true));
                            predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                            predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                            break;
                        case "EXPIRED":
                            predicates.add(cb.equal(root.get("active"), true));
                            predicates.add(cb.lessThan(root.get("endDate"), now));
                            break;
                        default:
                            throw new BusinessException("Invalid availabilityStatus: " + availabilityStatus,
                                    "PROMOTION_INVALID_QUERY", HttpStatus.BAD_REQUEST);
                    }
                }

                if (from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                }
                if (to != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            var page = campaignRepository.findAll(spec, pageable);

            List<CampaignListItemResponse> content = page.getContent().stream()
                    .map(campaign -> CampaignListItemResponse.builder()
                            .campaignId(campaign.getId())
                            .campaignName(campaign.getCampaignName())
                            .startDate(campaign.getStartDate())
                            .endDate(campaign.getEndDate())
                            .isActive(campaign.isActive())
                            .availabilityStatus(availabilityService.getCampaignStatus(campaign, now))
                            .createdAt(campaign.getCreatedAt())
                            .updatedAt(campaign.getUpdatedAt())
                            .build())
                    .toList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: GET_CAMPAIGNS, Count: {}, Duration: {}ms",
                    actorId, content.size(), duration);

            return CampaignPageResponse.builder()
                    .content(content)
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_CAMPAIGNS, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_CAMPAIGNS, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            PromotionCampaign campaign = campaignRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Campaign not found with id: " + id, "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

            long count = promotionRepository.countByCampaignId(id);
            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getCampaignStatus(campaign, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: GET_CAMPAIGN_DETAIL, CampaignId: {}, Duration: {}ms",
                    actorId, id, duration);

            return CampaignResponse.builder()
                    .campaignId(campaign.getId())
                    .campaignName(campaign.getCampaignName())
                    .description(campaign.getDescription())
                    .startDate(campaign.getStartDate())
                    .endDate(campaign.getEndDate())
                    .isActive(campaign.isActive())
                    .promotionCount((int) count)
                    .availabilityStatus(availability)
                    .createdAt(campaign.getCreatedAt())
                    .updatedAt(campaign.getUpdatedAt())
                    .build();

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_CAMPAIGN_DETAIL, CampaignId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: GET_CAMPAIGN_DETAIL, CampaignId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            PromotionCampaign campaign = campaignRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Campaign not found with id: " + id, "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

            // Validate date range
            if (request.getEndDate().isBefore(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate())) {
                throw new BusinessException("endDate must be after startDate", "CAMPAIGN_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
            }

            // Update campaign properties
            String campaignName = request.getCampaignName() != null ? request.getCampaignName().trim() : null;
            if (campaignName == null || campaignName.isEmpty()) {
                throw new BusinessException("campaignName is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }

            campaign.setCampaignName(campaignName);
            campaign.setDescription(request.getDescription());
            campaign.setStartDate(request.getStartDate());
            campaign.setEndDate(request.getEndDate());
            campaign.setActive(request.getIsActive() != null ? request.getIsActive() : true);

            PromotionCampaign savedCampaign = campaignRepository.save(campaign);
            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getCampaignStatus(savedCampaign, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: UPDATE_CAMPAIGN, CampaignId: {}, Duration: {}ms",
                    actorId, id, duration);

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
            log.error("Actor: {}, Operation: UPDATE_CAMPAIGN, CampaignId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: UPDATE_CAMPAIGN, CampaignId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CampaignResponse updateCampaignStatus(Long id, UpdateCampaignStatusRequest request) {
        long startTime = System.currentTimeMillis();
        String actorId = getCurrentActorId();

        try {
            PromotionCampaign campaign = campaignRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Campaign not found with id: " + id, "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

            campaign.setActive(request.getIsActive());
            PromotionCampaign savedCampaign = campaignRepository.save(campaign);

            LocalDateTime now = LocalDateTime.now();
            String availability = availabilityService.getCampaignStatus(savedCampaign, now);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Actor: {}, Operation: UPDATE_CAMPAIGN_STATUS, CampaignId: {}, Status: {}, Duration: {}ms",
                    actorId, id, request.getIsActive(), duration);

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
            log.error("Actor: {}, Operation: UPDATE_CAMPAIGN_STATUS, CampaignId: {}, Duration: {}ms, ErrorCode: {}, Message: {}",
                    actorId, id, duration, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Actor: {}, Operation: UPDATE_CAMPAIGN_STATUS, CampaignId: {}, Duration: {}ms, ErrorCode: INTERNAL_SERVER_ERROR, Message: {}",
                    actorId, id, duration, e.getMessage(), e);
            throw e;
        }
    }
}
