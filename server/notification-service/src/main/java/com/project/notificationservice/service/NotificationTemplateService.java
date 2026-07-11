package com.project.notificationservice.service;

import com.project.notificationservice.dto.request.CreateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateRequest;
import com.project.notificationservice.dto.response.NotificationTemplateResponse;
import com.project.notificationservice.dto.response.NotificationTemplateSummaryResponse;
import com.project.notificationservice.enums.NotificationChannel;
import org.springframework.data.domain.Page;

public interface NotificationTemplateService {

    NotificationTemplateResponse createTemplate(CreateNotificationTemplateRequest request);

    Page<NotificationTemplateSummaryResponse> getTemplateList(int page, int size, String code,
                                                             NotificationChannel channelType, Boolean isActive, String sort);

    NotificationTemplateResponse getTemplateDetail(Integer templateId);

    NotificationTemplateResponse updateTemplate(Integer templateId, UpdateNotificationTemplateRequest request);
}
