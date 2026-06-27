package com.project.notificationservice.mapper;

import com.project.notificationservice.entity.NotificationTemplate;
import com.project.notificationservice.dto.response.NotificationTemplateResponse;
import com.project.notificationservice.dto.response.NotificationTemplateSummaryResponse;

public class NotificationTemplateMapper {

    public static NotificationTemplateResponse toResponse(NotificationTemplate entity) {
        if (entity == null) {
            return null;
        }
        NotificationTemplateResponse response = new NotificationTemplateResponse();
        response.setTemplateId(entity.getId());
        response.setTemplateCode(entity.getTemplateCode());
        response.setTitle(entity.getTitle());
        response.setContent(entity.getContent());
        response.setChannelType(entity.getChannelType());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setVersion(entity.getVersion());
        return response;
    }

    public static NotificationTemplateSummaryResponse toSummaryResponse(NotificationTemplate entity) {
        if (entity == null) {
            return null;
        }
        NotificationTemplateSummaryResponse response = new NotificationTemplateSummaryResponse();
        response.setTemplateId(entity.getId());
        response.setTemplateCode(entity.getTemplateCode());
        response.setTitle(entity.getTitle());
        response.setChannelType(entity.getChannelType());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
