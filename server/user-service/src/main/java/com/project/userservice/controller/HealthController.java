package com.project.userservice.controller;

import com.project.userservice.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "User service is running");
    }
}
