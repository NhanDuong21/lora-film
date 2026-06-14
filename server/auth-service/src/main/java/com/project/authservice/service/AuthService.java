package com.project.authservice.service;

import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegisterResponse;

public interface AuthService {
	/**
	 * Registers a new user account.
	 *
	 * @param request registration request
	 * @return register response
	 */
	RegisterResponse register(RegisterRequest request);

	/**
	 * Authenticates user and generates JWT token.
	 *
	 * @param request login request
	 * @return jwt response
	 */
	JwtResponse login(LoginRequest request);

	/**
	 * Verifies account with OTP.
	 *
	 * @param request verification request
	 */
	void verify(VerifyRequest request);

	/**
	 * Refreshes an access token using a refresh token.
	 *
	 * @param request refresh request
	 * @return new jwt response with rotated tokens
	 */
	JwtResponse refreshToken(RefreshTokenRequest request);
}