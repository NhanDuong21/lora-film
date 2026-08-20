package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.request.LegalReviewRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;

public interface CampaignService {

    CampaignResponse createCampaign(CampaignCreateRequest request, String creator);

    CampaignResponse createCampaign(
            CampaignCreateRequest request, String creator,
            CampaignScopeType scopeType, Collection<String> cinemaScope);

    CampaignResponse updateCampaign(String publicId, CampaignUpdateRequest request, String updater);

    void deleteCampaign(String publicId, String deleter);

    CampaignDetailResponse getCampaign(String publicId);

    PagedResponse<CampaignResponse> searchCampaigns(String name, String code, CampaignStatus status,
                                                    Instant from, Instant to, Pageable pageable);

    PagedResponse<CampaignResponse> searchCampaigns(
            String name, String code, CampaignStatus status,
            Instant from, Instant to, Pageable pageable,
            Collection<String> accessibleCampaignIds);

    CampaignResponse submitCampaign(String publicId, String comment, String user);

    CampaignResponse submitCampaign(
            String publicId, String comment, String user,
            boolean approveImmediately);

    CampaignResponse publishCampaign(String publicId, String user);

    CampaignResponse activateCampaign(String publicId, String user);

    CampaignResponse pauseCampaign(String publicId, String user);

    CampaignResponse killSwitchCampaign(String publicId, String reason, String user);

    CampaignResponse cancelCampaign(String publicId, String user);

    CampaignResponse reviewLegalStatus(
            String publicId, LegalReviewRequest request, String reviewer);
}
