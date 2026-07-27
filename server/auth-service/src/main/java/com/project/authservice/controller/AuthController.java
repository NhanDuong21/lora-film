package com.project.authservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegistrationInitiatedResponse;
import com.project.authservice.service.AuthService;
import com.project.authservice.service.VerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
	private final AuthService authService;
	private final VerificationService verificationService;
	private final com.project.authservice.util.JwtUtil jwtUtil;

	/**
	 * Registers a new user.
	 *
	 * @param request registration request payload
	 * @return register response wrapped in ApiResponse
	 */
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegistrationInitiatedResponse>> register(@Valid @RequestBody RegisterRequest request) {
		log.info("Register endpoint called for email={}", request.getEmail());
		RegistrationInitiatedResponse response = authService.register(request);
		return ResponseEntity.ok(ApiResponse.success("Registration initiated", response));
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
		log.info("Verify endpoint called for email={}", request.getEmail());
		authService.verify(request);
		return ResponseEntity.ok(ApiResponse.success("Account verified successfully", null));
	}

	/**
	 * Sends a new OTP.
	 *
	 * @param request send otp request payload
	 * @return success response wrapped in ApiResponse
	 */
	@PostMapping("/send-otp")
	public ResponseEntity<ApiResponse<com.project.authservice.dto.response.SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
		log.info("Send OTP endpoint called for email={}", request.getEmail());
		com.project.authservice.dto.response.SendOtpResponse response = verificationService.sendOtp(request);
		return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
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

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(jakarta.servlet.http.HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			authService.logout(token);
		}
		return ResponseEntity.ok(ApiResponse.success("Logout successfully", null));
	}

	@PostMapping("/logout-all")
	public ResponseEntity<ApiResponse<Void>> logoutAll(jakarta.servlet.http.HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			String email = jwtUtil.extractUsername(token);
			authService.logoutAll(email);
		}
		return ResponseEntity.ok(ApiResponse.success("Logged out from all devices", null));
	}
	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.project.authservice.dto.request.ForgotPasswordRequest request) {
		log.info("Forgot password endpoint called for email={}", request.getEmail());
		authService.forgotPassword(request);
		return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.project.authservice.dto.request.ResetPasswordRequest request) {
		log.info("Reset password endpoint called");
		authService.resetPassword(request);
		return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
	}

	@PostMapping("/change-password")
	@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody com.project.authservice.dto.request.ChangePasswordRequest request, @org.springframework.security.core.annotation.AuthenticationPrincipal String username) {
		log.info("Change password endpoint called for user={}", username);
		authService.changePassword(request, username);
		return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
	}

	@GetMapping("/me")
	@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<com.project.authservice.dto.AccountDto>> getMe(@org.springframework.security.core.annotation.AuthenticationPrincipal String username) {
		log.info("Get me endpoint called for user={}", username);
		com.project.authservice.dto.AccountDto account = ((com.project.authservice.service.AccountService) org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext(
				((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getServletContext()
		).getBean(com.project.authservice.service.AccountService.class)).getAccountByEmail(username);
		return ResponseEntity.ok(ApiResponse.success("Success", account));
	}
    public AuthController(AuthService authService, VerificationService verificationService, com.project.authservice.util.JwtUtil jwtUtil) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.jwtUtil = jwtUtil;
    }
}