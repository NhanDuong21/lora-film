package com.project.paymentservice.service;

import com.project.paymentservice.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("payment-service", "UP");
	}
}
