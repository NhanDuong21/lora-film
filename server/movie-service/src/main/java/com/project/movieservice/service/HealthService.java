package com.project.movieservice.service;

import com.project.movieservice.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse health() {
		return new HealthResponse("movie-service", "UP");
	}
}
