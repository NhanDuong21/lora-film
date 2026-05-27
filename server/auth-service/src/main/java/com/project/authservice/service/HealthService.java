package com.project.authservice.service;

import com.project.authservice.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("auth-service", "UP");
	}
}
