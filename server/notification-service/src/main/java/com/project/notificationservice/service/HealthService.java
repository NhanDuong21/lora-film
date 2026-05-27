package com.project.notificationservice.service;

import com.project.notificationservice.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("notification-service", "UP");
	}
}
