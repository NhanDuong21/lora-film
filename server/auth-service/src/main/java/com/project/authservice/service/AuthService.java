package com.project.authservice.service;

import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.response.RegistrationInitiatedResponse;
import com.project.authservice.dto.ValidationResult;

public interface AuthService {
	/**
	 * Initiates a new user account registration.
	 *
	 * @param request registration request
	 * @return registration initiated response
	 */
	RegistrationInitiatedResponse register(RegisterRequest request);

	/**
	 * Completes the validation request.
	 * @param requestId the request ID
	 * @param result the validation result
	 */
	void completeValidation(String requestId, ValidationResult result);

	/**
	 * Verifies account registration.
	 *
	 * @param request verification request
	 */
	void verify(VerifyRequest request);

	/**
	 * Authenticates user and generates JWT token.
	 *
	 * @param request login request
	 * @return jwt response
	 */
	JwtResponse login(LoginRequest request);

	/**
	 * Refreshes an access token using a refresh token.
	 *
	 * @param request refresh request
	 * @return new jwt response with rotated tokens
	 */
	JwtResponse refreshToken(RefreshTokenRequest request);
}