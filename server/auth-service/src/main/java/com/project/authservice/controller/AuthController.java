package com.project.authservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.ResendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegisterResponse;
import com.project.authservice.service.AuthService;
import com.project.authservice.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final VerificationService verificationService;

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

	/**
	 * Verifies account registration via OTP.
	 *
	 * @param request verification request payload
	 * @return success response wrapped in ApiResponse
	 */
	@PostMapping("/verify")
	public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyRequest request) {
		log.info("Verify endpoint called for accountId={}", request.getAccountId());
		verificationService.verify(request);
		return ResponseEntity.ok(ApiResponse.success("Account verified successfully", null));
	}

	/**
	 * Sends a new OTP.
	 *
	 * @param request send otp request payload
	 * @return success response wrapped in ApiResponse
	 */
	@PostMapping("/send-otp")
	public ResponseEntity<ApiResponse<com.project.authservice.dto.response.ResendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
		log.info("Send OTP endpoint called for email={}", request.getEmail());
		com.project.authservice.dto.response.ResendOtpResponse response = verificationService.sendOtp(request);
		return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
	}

	/**
	 * Resends an OTP (equivalent to refresh-otp).
	 *
	 * @param request resend otp request payload
	 * @return success response wrapped in ApiResponse
	 */
	@PostMapping("/resend-otp")
	public ResponseEntity<ApiResponse<com.project.authservice.dto.response.ResendOtpResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
		log.info("Resend OTP endpoint called for email={}", request.getEmail());
		com.project.authservice.dto.response.ResendOtpResponse response = verificationService.resendOtp(request);
		return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", response));
	}

	/**
	 * Refreshes JWT token.
	 *
	 * @param request refresh token request payload
	 * @return new jwt response wrapped in ApiResponse
	 */
	@PostMapping("/refresh-token")
	public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		log.info("Refresh token endpoint called");
		JwtResponse response = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
	}
}