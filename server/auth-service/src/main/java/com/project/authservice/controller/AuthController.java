package com.project.authservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.JwtResponse;
import com.project.authservice.dto.LoginRequest;
import com.project.authservice.dto.RegisterRequest;
import com.project.authservice.dto.RegisterResponse;
import com.project.authservice.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	/**
	 * Registers a new user.
	 *
	 * @param request registration request payload
	 * @return register response wrapped in ApiResponse
	 */
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
		log.info("Register endpoint called for email={}", request.getEmail());
		RegisterResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Register successfully", response));
	}

	/**
	 * Authenticates a user.
	 *
	 * @param request login request payload
	 * @return jwt response wrapped in ApiResponse
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
		log.info("Login endpoint called for email={}", request.getEmail());
		JwtResponse response = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success("Login successfully", response));
	}
}