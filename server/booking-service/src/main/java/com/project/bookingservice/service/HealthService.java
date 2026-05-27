package com.project.bookingservice.service;

import com.project.bookingservice.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("booking-service", "UP");
	}
}
