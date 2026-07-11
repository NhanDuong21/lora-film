package com.project.notificationservice.service;

import com.project.notificationservice.dto.request.SendNotificationRequest;
import com.project.notificationservice.dto.response.SendNotificationResponse;

public interface NotificationService {

    SendNotificationResponse sendNotification(SendNotificationRequest request);
}
