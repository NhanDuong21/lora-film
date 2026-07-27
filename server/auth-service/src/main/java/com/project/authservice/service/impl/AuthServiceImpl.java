package com.project.authservice.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.event.publisher.AuthAccountEventPublisher;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegistrationInitiatedResponse;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.ValidationResult;
import com.project.authservice.entity.PendingRegistrationData;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.entity.RefreshToken;
import com.project.authservice.exception.AccountInactiveException;
import com.project.authservice.exception.AccountNotVerifiedException;
import com.project.authservice.exception.InvalidBirthdayFormatException;
import com.project.authservice.exception.InvalidRefreshTokenException;
import com.project.authservice.exception.CccdException.BirthdayCccdMismatchException;
import com.project.authservice.exception.EmailAlreadyExistsException;
import com.project.authservice.exception.InvalidCredentialsException;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.exception.DuplicateResourceException;
import com.project.authservice.exception.RegistrationConflictException;
import com.project.authservice.exception.RegistrationAlreadyPendingException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.repository.RefreshTokenRepository;
import com.project.authservice.service.AuthService;
import com.project.authservice.service.VerificationService;
import com.project.authservice.service.AuditLogService;
import com.project.authservice.util.JwtUtil;
import com.project.authservice.util.RefreshTokenHashUtil;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final String CUSTOMER_ROLE = "CUSTOMER";

	private final AccountRepository accountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final CccdCheckClient cccdCheckClient;
	private final VerificationService verificationService;
	private final AuditLogService auditLogService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final HttpServletRequest servletRequest;
	private final AuthAccountEventPublisher eventPublisher;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final com.project.authservice.repository.UserSessionRepository userSessionRepository;
	private final com.project.authservice.repository.LoginHistoryRepository loginHistoryRepository;
	private final com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;
	private final ConcurrentHashMap<String, CompletableFuture<ValidationResult>> pendingRequests = new ConcurrentHashMap<>();
	

	public AuthServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
			CccdCheckClient cccdCheckClient,
			VerificationService verificationService, AuditLogService auditLogService,
			RefreshTokenRepository refreshTokenRepository, HttpServletRequest servletRequest,
			AuthAccountEventPublisher eventPublisher, StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
			com.project.authservice.repository.UserSessionRepository userSessionRepository,
			com.project.authservice.repository.LoginHistoryRepository loginHistoryRepository,
			com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository) {
		this.accountRepository = accountRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
		this.cccdCheckClient = cccdCheckClient;
		this.verificationService = verificationService;
		this.auditLogService = auditLogService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.servletRequest = servletRequest;
		this.eventPublisher = eventPublisher;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.userSessionRepository = userSessionRepository;
		this.loginHistoryRepository = loginHistoryRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
	}

	@Transactional
	public void completeValidation(String requestId, ValidationResult result) {
		CompletableFuture<ValidationResult> future = pendingRequests.get(requestId);
		if (future != null) {
			future.complete(result);
		} else {
			log.warn("No pending registration request found for requestId={}", requestId);
		}
	}

	@Override
	public RegistrationInitiatedResponse register(RegisterRequest request) {
		try {
			String email = request.getEmail().trim().toLowerCase();
			log.info("Register request received for email={}", email);

			Account existingAccount = accountRepository.findByEmail(email).orElse(null);
			if (existingAccount != null) {
				if (existingAccount.getAccountStatus() != com.project.authservice.enums.AccountStatus.INACTIVE) {
					log.warn("Email already registered and verified: {}", email);
					throw new EmailAlreadyExistsException();
				} else {
					String pendingKeyCheck = "pending_registration:" + email;
					if (Boolean.TRUE.equals(redisTemplate.hasKey(pendingKeyCheck))) {
						log.warn("Registration already pending for email: {}", email);
						throw new RegistrationAlreadyPendingException("Registration is already pending verification. Please verify the OTP or request a new OTP.");
					} else {
						// Registration expired, allow overriding
						log.info("Registration expired for email: {}. Overriding previous unverified account.", email);
					}
				}
			} else {
				// Also check pending key just in case account hasn't been created yet but is in flow
				String pendingKeyCheck = "pending_registration:" + email;
				if (Boolean.TRUE.equals(redisTemplate.hasKey(pendingKeyCheck))) {
					log.warn("Registration already pending for email: {}", email);
					throw new RegistrationAlreadyPendingException("Registration is already pending verification. Please verify the OTP or request a new OTP.");
				}
			}

			// Perform CCCD Check and info derivation
			CccdCheckClient.CccdInfo cccdInfo = cccdCheckClient.checkCccd(request.getCccd());

			// Verify birthday matches birth year derived from CCCD.
			String birthdayStr = request.getBirthday().trim();
			LocalDate birthday;
			try {
				birthday = LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE);
			} catch (DateTimeParseException e) {
				log.warn("Invalid birthday format '{}': {}", birthdayStr, e.getMessage());
				throw new InvalidBirthdayFormatException();
			}

			if (birthday.getYear() != cccdInfo.getBirthYear()) {
				log.warn("Birthday birth year {} does not match CCCD birth year {}",
						birthday.getYear(), cccdInfo.getBirthYear());
				throw new BirthdayCccdMismatchException();
			}

			if (birthday.isAfter(LocalDate.now())) {
				log.warn("Birthday cannot be in the future: {}", birthday);
				throw new InvalidBirthdayFormatException("Birth dates cannot be in the future.");
			}

			LocalDate today = LocalDate.now();
			int age = today.getYear() - birthday.getYear();
			if (birthday.plusYears(age).isAfter(today)) {
				age--;
			}
			if (age < 13) {
				log.warn("Age under 13: {}", age);
				throw new InvalidBirthdayFormatException("You must be 13 years old or older.");
			}

			request.setPassword(passwordEncoder.encode(request.getPassword()));

			String requestId = UUID.randomUUID().toString();
			PendingRegistrationData pendingData = new PendingRegistrationData(request, cccdInfo);
			String json;

			try {
				json = objectMapper.writeValueAsString(pendingData);
				redisTemplate.opsForValue().set("temp_request:" + requestId, json, Duration.ofMinutes(5));
			} catch (Exception e) {
				log.error("Failed to save temp registration request to Redis", e);
				throw new RuntimeException("Internal error processing registration");
			}

			CompletableFuture<ValidationResult> future = new CompletableFuture<>();
			pendingRequests.put(requestId, future);

			try {
				eventPublisher.publishRegistrationValidationRequested(request, requestId);
				
				ValidationResult result = future.get(10, TimeUnit.SECONDS);
				
				if ("SUCCESS".equalsIgnoreCase(result.getStatus())) {
					Role role = roleRepository.findByRoleName(CUSTOMER_ROLE)
							.orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found"));

					Account existingForUpdate = accountRepository.findByEmail(email).orElse(null);
					Account account = existingForUpdate != null ? existingForUpdate : new Account();
					account.setEmail(email);
					account.setPasswordHash(request.getPassword()); // already encoded
					account.setRole(role);
					account.setAccountStatus(com.project.authservice.enums.AccountStatus.INACTIVE);
					accountRepository.save(account);

					String pendingKey = "pending_registration:" + email;
					redisTemplate.opsForValue().set(pendingKey, json, Duration.ofMinutes(15));
					verificationService.sendOtp(new SendOtpRequest(email));
					
					return new RegistrationInitiatedResponse(requestId, "Registration successful, please check your email for OTP");
				} else {
					if ("PHONE_NUMBER_AND_CCCD_ALREADY_EXIST".equals(result.getErrorCode())) {
					    throw new RegistrationConflictException("Registration information (Phone number or CCCD) already exists.", "VALIDATION_ERROR", null,
					            List.of(
					                    new com.project.authservice.common.ApiResponse.ValidationError("phoneNumber", "Duplicate", "Phone number already exists or is reserved."),
					                    new com.project.authservice.common.ApiResponse.ValidationError("cccd", "Duplicate", "CCCD already exists or is reserved.")
					            ));
					} else if ("PHONE_NUMBER_ALREADY_EXISTS".equals(result.getErrorCode())) {
					    throw new RegistrationConflictException("Phone number already exists.", "PHONE_NUMBER_ALREADY_EXISTS", null, null);
					} else if ("CCCD_ALREADY_EXISTS".equals(result.getErrorCode())) {
					    throw new RegistrationConflictException("CCCD already exists.", "CCCD_ALREADY_EXISTS", null, null);
					} else if ("PHONE_NUMBER_RESERVED".equals(result.getErrorCode())) {
						throw new RegistrationConflictException("Phone number is currently reserved by another pending registration. Please try again later.", result.getErrorCode(), result.getRetryAfterSeconds(), null);
					} else if ("CCCD_RESERVED".equals(result.getErrorCode())) {
						throw new RegistrationConflictException("CCCD is currently reserved by another pending registration. Please try again later.", result.getErrorCode(), result.getRetryAfterSeconds(), null);
					} else {
						throw new RegistrationConflictException("Registration conflict", result.getErrorCode(), result.getRetryAfterSeconds(), null);
					}
				}
			} catch (TimeoutException e) {
				throw new com.project.authservice.exception.common.GatewayTimeoutException("Gateway timeout. Please try again later.");
			} catch (RegistrationConflictException e) {
				throw e;
			} catch (DuplicateResourceException e) {
				auditLogService.log(null, "REGISTER_FAILED_DUPLICATE", servletRequest);
				throw e;
			} catch (Exception e) {
				log.error("Error during registration validation", e);
				auditLogService.log(null, "REGISTER_FAILED", servletRequest);
				throw new RuntimeException("System overload. Failed to validate registration. Please try again later.", e);
			} finally {
				pendingRequests.remove(requestId);
				redisTemplate.delete("temp_request:" + requestId);
			}
		} catch (RegistrationConflictException | RegistrationAlreadyPendingException e) {
			throw e;
		} catch (DuplicateResourceException e) {
			auditLogService.log(null, "REGISTER_FAILED_DUPLICATE", servletRequest);
			throw e;
		} catch (Exception e) {
			auditLogService.log(null, "REGISTER_FAILED", servletRequest);
			throw e;
		}
	}

	@Override
	@Transactional
	public void verify(VerifyRequest request) {
		verificationService.verify(request);

		String email = request.getEmail();
		String pendingKey = "pending_registration:" + email;

		String json = redisTemplate.opsForValue().get(pendingKey);
		if (json == null) {
			return; // Not a registration verification, just regular OTP verification
		}

		PendingRegistrationData data;
		try {
			data = objectMapper.readValue(json, PendingRegistrationData.class);
		} catch (Exception e) {
			log.error("Failed to deserialize pending registration data", e);
			throw new RuntimeException("Internal error");
		}

		RegisterRequest registerRequest = data.getRequest();
		CccdCheckClient.CccdInfo cccdInfo = data.getCccdInfo();

		Account savedAccount = accountRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + email));

		// Update status to VERIFIED (OTP is correct, waiting for User Profile creation)
		savedAccount.setAccountStatus(com.project.authservice.enums.AccountStatus.ACTIVE);
		accountRepository.save(savedAccount);

		log.info("Account verified successfully for email={} with accountId={}", email, savedAccount.getId());
		auditLogService.log(savedAccount.getId(), "REGISTER_SUCCESS", servletRequest);

		try {
			eventPublisher.publishAccountVerified(savedAccount, registerRequest, cccdInfo);
		} catch (Exception kafkaEx) {
			log.error("ACCOUNT_VERIFIED Kafka event failed for accountId={} email={}: {}",
					savedAccount.getId(), email, kafkaEx.getMessage(), kafkaEx);
			throw new RuntimeException("System overload. Failed to send profile creation request. Please try again later.", kafkaEx);
		}

		// DO NOT delete pendingKey here. It will be deleted by UserProfileCreatedConsumer after User Profile is created.
	}

	/**
	 * Authenticates user and generates JWT token.
	 *
	 * @param request login request
	 * @return jwt response
	 */
	@Override
	@Transactional
	public JwtResponse login(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		log.info("Login request received for email={}", email);

		// 1. Check account exists
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("Login failed: email {} not found", email);
					auditLogService.log(null, "LOGIN_FAILED_INVALID_PASSWORD", servletRequest);
					return new InvalidCredentialsException();
				});

		// 2. Check password
		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			log.warn("Login failed: password mismatch for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_INVALID_PASSWORD", servletRequest);
			throw new InvalidCredentialsException();
		}

		// 3. Check account status
		if (account.getAccountStatus() == com.project.authservice.enums.AccountStatus.INACTIVE) {
			log.warn("Login failed: account is not verified for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_NOT_VERIFIED", servletRequest);
			throw new AccountNotVerifiedException(account.getId());
		}

		// 4. Check if account is active
		if (account.getAccountStatus() != com.project.authservice.enums.AccountStatus.ACTIVE) {
			log.warn("Login failed: account is inactive (status={}) for email {}", account.getAccountStatus(), email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_INACTIVE_ACCOUNT", servletRequest);
			throw new AccountInactiveException();
		}

		// 5. Revoke any existing active refresh tokens issued from the same browser/device.
		String currentUserAgent = servletRequest.getHeader("User-Agent");
		if (currentUserAgent != null && !currentUserAgent.isBlank()) {
			List<RefreshToken> sameDeviceTokens = refreshTokenRepository
					.findActiveTokensByAccountAndUserAgent(account.getId(), currentUserAgent);
			if (!sameDeviceTokens.isEmpty()) {
				sameDeviceTokens.forEach(t -> t.setIsRevoked(true));
				refreshTokenRepository.saveAll(sameDeviceTokens);
				log.info("Revoked {} active refresh token(s) for account {} from the same device",
						sameDeviceTokens.size(), email);
			}
		}

		// 6. Generate Tokens
		String accessToken = jwtUtil.generateToken(account.getId(), account.getEmail(), account.getRole().getRoleName());
		String plainRefreshToken = UUID.randomUUID().toString();
		String refreshTokenHash = RefreshTokenHashUtil.hash(plainRefreshToken);

		LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setAccount(account);
		refreshToken.setToken(refreshTokenHash);
		refreshToken.setExpiryDate(expiresAt);
		refreshToken.setIsRevoked(false);
		refreshTokenRepository.save(refreshToken);

		// 7. Save User Session
		com.project.authservice.entity.UserSession userSession = com.project.authservice.entity.UserSession.builder()
				.account(account)
				.refreshToken(refreshToken)
				.ipAddress(servletRequest.getRemoteAddr())
				.userAgent(currentUserAgent)
				.expiresAt(LocalDateTime.now().plusHours(24)) // 24 hours matches JWT expiration
				.isActive(true)
				.build();
		userSessionRepository.save(userSession);

		// 8. Save Login History
		com.project.authservice.entity.LoginHistory loginHistory = com.project.authservice.entity.LoginHistory.builder()
				.account(account)
				.ipAddress(servletRequest.getRemoteAddr())
				.userAgent(currentUserAgent)
				.status("SUCCESS")
				.build();
		loginHistoryRepository.save(loginHistory);

		// 9. Write Audit Log
		auditLogService.log(account.getId(), "LOGIN_SUCCESS", servletRequest);
		log.info("User {} logged in successfully", email);

		// 10. Return response
		long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
		return new JwtResponse(
				accessToken,
				plainRefreshToken,
				expiresInSeconds,
				account.getEmail(),
				account.getRole().getRoleName(),
				account.getId());
	}

	@Override
	@Transactional
	public JwtResponse loginOAuth2(Account account, HttpServletRequest request) {
		String email = account.getEmail();

		// Generate Tokens
		String accessToken = jwtUtil.generateToken(account.getId(), account.getEmail(), account.getRole().getRoleName());
		String plainRefreshToken = UUID.randomUUID().toString();
		String refreshTokenHash = RefreshTokenHashUtil.hash(plainRefreshToken);

		LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setAccount(account);
		refreshToken.setToken(refreshTokenHash);
		refreshToken.setExpiryDate(expiresAt);
		refreshToken.setIsRevoked(false);
		refreshTokenRepository.save(refreshToken);

		String currentUserAgent = request.getHeader("User-Agent");

		// Save User Session
		String sessionId = UUID.randomUUID().toString();
		com.project.authservice.entity.UserSession userSession = com.project.authservice.entity.UserSession.builder()
				.id(sessionId)
				.account(account)
				.accessTokenHash(RefreshTokenHashUtil.hash(accessToken))
				.ipAddress(request.getRemoteAddr())
				.userAgent(currentUserAgent)
				.expiresAt(LocalDateTime.now().plusHours(24))
				.isActive(true)
				.build();
		userSessionRepository.save(userSession);

		// Save Login History
		com.project.authservice.entity.LoginHistory loginHistory = com.project.authservice.entity.LoginHistory.builder()
				.account(account)
				.ipAddress(request.getRemoteAddr())
				.userAgent(currentUserAgent)
				.status("SUCCESS")
				.build();
		loginHistoryRepository.save(loginHistory);

		// Write Audit Log
		auditLogService.log(account.getId(), "OAUTH2_LOGIN_SUCCESS", request);
		log.info("User {} logged in via OAuth2 successfully", email);

		long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
		return new JwtResponse(
				accessToken,
				plainRefreshToken,
				expiresInSeconds,
				account.getEmail(),
				account.getRole().getRoleName(),
				account.getId());
	}

	/**
	 * Refreshes an access token using a refresh token.
	 *
	 * @param request refresh request
	 * @return new jwt response with rotated tokens
	 */
	@Override
	@Transactional
	public JwtResponse refreshToken(RefreshTokenRequest request) {
		String tokenStr = request.getRefreshToken();
		String hashedToken = RefreshTokenHashUtil.hash(tokenStr);
		try {
			RefreshToken refreshToken = refreshTokenRepository.findByToken(hashedToken)
					.orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

			if (refreshToken.getIsRevoked() == null || refreshToken.getIsRevoked()) {
				throw new InvalidRefreshTokenException("Refresh token is revoked");
			}

			if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
				throw new InvalidRefreshTokenException("Refresh token is expired");
			}

			Account account = refreshToken.getAccount();
			if (account == null) {
				throw new InvalidRefreshTokenException("Account not found");
			}

			if (account.getAccountStatus() == com.project.authservice.enums.AccountStatus.INACTIVE) {
				throw new AccountNotVerifiedException(account.getId());
			}

			if (account.getAccountStatus() != com.project.authservice.enums.AccountStatus.ACTIVE) {
				throw new AccountInactiveException();
			}

			// Calculate remaining duration until expiryDate
			LocalDateTime now = LocalDateTime.now();
			Duration remainingDuration = Duration.between(now, refreshToken.getExpiryDate());

			// Generate new access token
			String newAccessToken = jwtUtil.generateToken(account.getId(), account.getEmail(),
					account.getRole().getRoleName());

			String responseRefreshToken;

			if (remainingDuration.compareTo(Duration.ofDays(5)) <= 0) {
				// Remaining duration is 5 days or less -> Rotate the refresh token
				refreshToken.setIsRevoked(true);
				refreshTokenRepository.save(refreshToken);

				// Generate new refresh token
				String newPlainRefreshToken = UUID.randomUUID().toString();
				RefreshToken newRefreshToken = new RefreshToken();
				newRefreshToken.setAccount(account);
				newRefreshToken.setToken(RefreshTokenHashUtil.hash(newPlainRefreshToken));
				newRefreshToken.setExpiryDate(now.plusDays(7));
				newRefreshToken.setIsRevoked(false);
				refreshTokenRepository.save(newRefreshToken);

				responseRefreshToken = newPlainRefreshToken;
			} else {
				// Remaining duration is greater than 5 days -> Keep the current refresh token
				responseRefreshToken = tokenStr;
			}

			auditLogService.log(account.getId(), "REFRESH_TOKEN_SUCCESS", servletRequest);

			long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
			return new JwtResponse(
					newAccessToken,
					responseRefreshToken,
					expiresInSeconds,
					account.getEmail(),
					account.getRole().getRoleName(),
					account.getId());
		} catch (Exception e) {
			Long accountId = null;
			try {
				RefreshToken tempToken = refreshTokenRepository.findByToken(hashedToken).orElse(null);
				if (tempToken != null && tempToken.getAccount() != null) {
					accountId = tempToken.getAccount().getId();
				}
			} catch (Exception ignore) {
			}
			auditLogService.log(accountId, "REFRESH_TOKEN_FAILED", servletRequest);
			throw e;
		}
	}


	@Override
	@Transactional
	public void logout(String token) {
		String email = jwtUtil.extractUsername(token);
		log.info("Logout request for email={}", email);
		
		// 1. Blacklist token in Redis
		long exp = jwtUtil.extractExpiration(token).getTime();
		long now = System.currentTimeMillis();
		String tokenHash = RefreshTokenHashUtil.hash(token);
		if (exp > now) {
			redisTemplate.opsForValue().set("blacklist:" + tokenHash, "revoked", Duration.ofMillis(exp - now));
		}
		
		// 2. Invalidate session
		// Access tokens are now solely managed by Redis Blacklist. Database session is linked to refresh token.
			
		// 3. Log Audit
		Account account = accountRepository.findByEmail(email).orElse(null);
		if (account != null) {
			auditLogService.log(account.getId(), "LOGOUT_SUCCESS", servletRequest);
		}
	}

	@Override
	@Transactional
	public void logoutAll(String email) {
		log.info("LogoutAll request for email={}", email);
		Account account = accountRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
			
		// Invalidate all active sessions
		userSessionRepository.revokeAllForAccount(account.getId());
		
		// Invalidate all refresh tokens
		java.util.List<com.project.authservice.entity.RefreshToken> activeTokens = refreshTokenRepository.findActiveTokensByAccountId(account.getId());
		activeTokens.forEach(t -> t.setIsRevoked(true));
		refreshTokenRepository.saveAll(activeTokens);
		
		auditLogService.log(account.getId(), "LOGOUT_ALL_SUCCESS", servletRequest);
	}
	@Override
	@Transactional
	public void forgotPassword(com.project.authservice.dto.request.ForgotPasswordRequest request) {
		log.info("Forgot password requested for email={}", request.getEmail());
		Account account = accountRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
		
		String otp = String.format("%06d", new java.util.Random().nextInt(999999));
		com.project.authservice.entity.PasswordResetToken resetToken = com.project.authservice.entity.PasswordResetToken.builder()
				.account(account)
				.otpCode(otp)
				.expiredAt(LocalDateTime.now().plusMinutes(15))
				.isUsed(false)
				.attempts(0)
				.build();
		passwordResetTokenRepository.save(resetToken);
		
		// In a real system, send email here
		log.info("Password reset OTP generated for email={}: {}", request.getEmail(), otp);
	}

	@Override
	@Transactional
	public void resetPassword(com.project.authservice.dto.request.ResetPasswordRequest request) {
		log.info("Reset password requested");
		com.project.authservice.entity.PasswordResetToken resetToken = passwordResetTokenRepository.findByOtpCode(request.getToken())
				.orElseThrow(() -> new RuntimeException("Invalid token"));
				
		if (resetToken.getIsUsed() || resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("Token expired or already used");
		}
		
		Account account = resetToken.getAccount();
		account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		accountRepository.save(account);
		
		resetToken.setIsUsed(true);
		resetToken.setUsedAt(LocalDateTime.now());
		passwordResetTokenRepository.save(resetToken);
		
		// Revoke all sessions for security
		userSessionRepository.revokeAllForAccount(account.getId());
		auditLogService.log(account.getId(), "PASSWORD_RESET_SUCCESS", servletRequest);
	}

	@Override
	@Transactional
	public void changePassword(com.project.authservice.dto.request.ChangePasswordRequest request, String email) {
		log.info("Change password requested for email={}", email);
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
				
		if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
			throw new RuntimeException("Old password incorrect");
		}
		
		account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		accountRepository.save(account);
		
		// Revoke all sessions except current one (if we had session ID, but let's just revoke all for security)
		userSessionRepository.revokeAllForAccount(account.getId());
		auditLogService.log(account.getId(), "PASSWORD_CHANGED", servletRequest);
	}
}
