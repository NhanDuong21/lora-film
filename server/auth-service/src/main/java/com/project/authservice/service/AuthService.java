package com.project.authservice.service;

import com.project.authservice.dto.JwtResponse;
import com.project.authservice.dto.LoginRequest;
import com.project.authservice.dto.RegisterRequest;
import com.project.authservice.dto.RegisterResponse;

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
}