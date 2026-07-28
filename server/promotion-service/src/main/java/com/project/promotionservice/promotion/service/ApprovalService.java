package com.project.promotionservice.promotion.service;

import com.project.promotionservice.promotion.dto.response.ApprovalHistoryResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;

import java.util.List;

public interface ApprovalService {

    CampaignResponse approveCampaign(String publicId, String comment, String approver, List<String> roles);

    CampaignResponse rejectCampaign(String publicId, String comment, String approver, List<String> roles);

    List<ApprovalHistoryResponse> getApprovalHistory(String targetPublicId);
}
