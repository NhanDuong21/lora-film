package com.project.apigateway.service;

import com.project.apigateway.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("api-gateway", "UP");
	}
}
