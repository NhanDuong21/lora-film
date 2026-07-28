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

	com.project.authservice.dto.response.RegistrationStatusResponse getRegistrationStatus(String requestId);

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

	/**
	 * Logs out the user by invalidating the current token.
	 *
	 * @param token jwt access token
	 */
	void logout(String token);

	/**
	 * Logs out the user from all sessions.
	 *
	 * @param email user email
	 */
	void logoutAll(String email);

	void forgotPassword(com.project.authservice.dto.request.ForgotPasswordRequest request);
	
	void resetPassword(com.project.authservice.dto.request.ResetPasswordRequest request);
	
	void changePassword(com.project.authservice.dto.request.ChangePasswordRequest request, String email);

	/**
	 * Authenticates an OAuth2 user and generates JWT token.
	 *
	 * @param account account
	 * @param request servlet request
	 * @return jwt response
	 */
	JwtResponse loginOAuth2(com.project.authservice.entity.Account account, jakarta.servlet.http.HttpServletRequest request);
}
