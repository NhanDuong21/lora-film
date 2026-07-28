package com.project.promotionservice.promotion.mapper;

import com.project.promotionservice.promotion.dto.response.ApprovalHistoryResponse;
import com.project.promotionservice.promotion.entity.ApprovalHistory;
import org.springframework.stereotype.Component;

@Component
public class ApprovalMapper {

    public ApprovalHistoryResponse toResponse(ApprovalHistory entity) {
        if (entity == null) {
            return null;
        }

        ApprovalHistoryResponse response = new ApprovalHistoryResponse();
        response.setPublicId(entity.getPublicId());
        response.setTargetType(entity.getTargetType());
        response.setTargetPublicId(entity.getTargetPublicId());
        response.setAction(entity.getAction());
        response.setOldStatus(entity.getOldStatus());
        response.setNewStatus(entity.getNewStatus());
        response.setApproverPublicId(entity.getApproverPublicId());
        response.setComment(entity.getComment());
        response.setApprovedAt(entity.getApprovedAt());
        response.setMetadataJson(entity.getMetadataJson());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());

        return response;
    }
}
