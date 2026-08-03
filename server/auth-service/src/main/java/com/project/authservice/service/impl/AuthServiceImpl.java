package com.project.authservice.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;

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
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
import com.project.authservice.service.AuthOutboxService;
import com.project.authservice.service.CredentialRevocationService;
import com.project.authservice.util.JwtUtil;
import com.project.authservice.util.RefreshTokenHashUtil;

import jakarta.servlet.http.HttpServletRequest;

import static com.project.authservice.util.SensitiveDataMasker.maskEmail;

@Service
public class AuthServiceImpl implements AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final String CUSTOMER_ROLE = "CUSTOMER";
	private static final DefaultRedisScript<Long> RELEASE_REGISTRATION_RESERVATION_SCRIPT =
			new DefaultRedisScript<>("""
					local deleted = 0
					if redis.call('GET', KEYS[1]) == ARGV[1] then
					  deleted = deleted + redis.call('DEL', KEYS[1])
					end
					if redis.call('GET', KEYS[2]) == ARGV[1] then
					  deleted = deleted + redis.call('DEL', KEYS[2])
					end
					return deleted
					""", Long.class);

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
	private final CredentialRevocationService credentialRevocationService;
	private final AuthOutboxService authOutboxService;
	private final SecureRandom secureRandom = new SecureRandom();
	private final ConcurrentHashMap<String, CompletableFuture<ValidationResult>> pendingRequests = new ConcurrentHashMap<>();
	

	public AuthServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
			CccdCheckClient cccdCheckClient,
			VerificationService verificationService, AuditLogService auditLogService,
			RefreshTokenRepository refreshTokenRepository, HttpServletRequest servletRequest,
			AuthAccountEventPublisher eventPublisher, StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
			com.project.authservice.repository.UserSessionRepository userSessionRepository,
			com.project.authservice.repository.LoginHistoryRepository loginHistoryRepository,
			com.project.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository,
			CredentialRevocationService credentialRevocationService,
			AuthOutboxService authOutboxService) {
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
		this.credentialRevocationService = credentialRevocationService;
		this.authOutboxService = authOutboxService;
	}

	@Transactional
	public void completeValidation(String requestId, ValidationResult result) {
		try {
			redisTemplate.opsForValue().set(
					"registration_validation_result:" + requestId,
					objectMapper.writeValueAsString(result),
					Duration.ofSeconds(30));
		} catch (Exception exception) {
			log.error("Failed to persist registration validation result for requestId={}", requestId, exception);
		}
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
			request.setEmail(email);
			log.info("Register request received for email={}", maskEmail(email));

			Account existingAccount = accountRepository.findByEmail(email).orElse(null);
			if (existingAccount != null) {
				if (existingAccount.getAccountStatus() != com.project.authservice.enums.AccountStatus.INACTIVE) {
					log.warn("Email already registered and verified: {}", maskEmail(email));
					throw new EmailAlreadyExistsException();
				} else {
					String pendingKeyCheck = "pending_registration:" + email;
					if (Boolean.TRUE.equals(redisTemplate.hasKey(pendingKeyCheck))) {
						log.warn("Registration already pending for email: {}", maskEmail(email));
						throw new RegistrationAlreadyPendingException("Registration is already pending verification. Please verify the OTP or request a new OTP.");
					} else {
						// Registration expired, allow overriding
						log.info("Registration expired for email: {}. Overriding previous unverified account.", maskEmail(email));
					}
				}
			} else {
				// Also check pending key just in case account hasn't been created yet but is in flow
				String pendingKeyCheck = "pending_registration:" + email;
				if (Boolean.TRUE.equals(redisTemplate.hasKey(pendingKeyCheck))) {
					log.warn("Registration already pending for email: {}", maskEmail(email));
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
			boolean validationRequested = false;
			boolean registrationInitiated = false;
			boolean pendingRegistrationStored = false;
			String pendingKey = "pending_registration:" + email;

			try {
				eventPublisher.publishRegistrationValidationRequested(request, requestId);
				validationRequested = true;
				
				ValidationResult result = waitForValidation(requestId, future);
				
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

					redisTemplate.opsForValue().set(pendingKey, json, Duration.ofMinutes(15));
					pendingRegistrationStored = true;
					verificationService.sendOtp(new SendOtpRequest(email));
					registrationInitiated = true;

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
					} else if ("PHONE_NUMBER_AND_CCCD_RESERVED".equals(result.getErrorCode())) {
						throw new RegistrationConflictException("Phone number and CCCD are currently reserved by another pending registration. Please try again later.", result.getErrorCode(), result.getRetryAfterSeconds(), null);
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
				if (!registrationInitiated) {
					if (pendingRegistrationStored) {
						redisTemplate.delete(pendingKey);
					}
					if (validationRequested) {
						releaseRegistrationReservation(request, email);
					}
				}
				pendingRequests.remove(requestId);
				redisTemplate.delete("temp_request:" + requestId);
				redisTemplate.delete("registration_validation_result:" + requestId);
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

	private void releaseRegistrationReservation(RegisterRequest request, String reservationOwner) {
		try {
			redisTemplate.execute(
					RELEASE_REGISTRATION_RESERVATION_SCRIPT,
					List.of(
							"reserved_phone:" + request.getPhoneNumber(),
							"reserved_cccd:" + request.getCccd()),
					reservationOwner);
		} catch (Exception cleanupException) {
			log.error("Failed to release registration reservation for email={}",
					maskEmail(reservationOwner), cleanupException);
		}
	}

	@Override
	@Transactional
	public void verify(VerifyRequest request) {
		request.setEmail(normalizeEmail(request.getEmail()));
		verificationService.verify(request);

		String email = normalizeEmail(request.getEmail());
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

		log.info("Account verified successfully for email={} with accountId={}", maskEmail(email), savedAccount.getId());
		auditLogService.log(savedAccount.getId(), "REGISTER_SUCCESS", servletRequest);

		try {
			eventPublisher.publishAccountVerified(savedAccount, registerRequest, cccdInfo);
		} catch (Exception kafkaEx) {
			log.error("ACCOUNT_VERIFIED Kafka event failed for accountId={} email={}: {}",
					savedAccount.getId(), maskEmail(email), kafkaEx.getMessage(), kafkaEx);
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
		String email = normalizeEmail(request.getEmail());
		log.info("Login request received for email={}", maskEmail(email));
		enforceLoginRateLimit(email);

		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("Login failed: email {} not found", maskEmail(email));
					recordFailedLogin(email);
					auditLogService.log(null, "LOGIN_FAILED_INVALID_PASSWORD", servletRequest);
					return new InvalidCredentialsException();
				});

		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			log.warn("Login failed: password mismatch for email {}", maskEmail(email));
			recordFailedLogin(email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_INVALID_PASSWORD", servletRequest);
			throw new InvalidCredentialsException();
		}

		if (account.getAccountStatus() == com.project.authservice.enums.AccountStatus.INACTIVE) {
			log.warn("Login failed: account is not verified for email {}", maskEmail(email));
			auditLogService.log(account.getId(), "LOGIN_FAILED_NOT_VERIFIED", servletRequest);
			throw new AccountNotVerifiedException(account.getId());
		}

		if (account.getAccountStatus() != com.project.authservice.enums.AccountStatus.ACTIVE
				|| !Boolean.TRUE.equals(account.getIsEnabled())
				|| Boolean.TRUE.equals(account.getIsDeleted())) {
			log.warn("Login failed: account is inactive (status={}) for email {}",
					account.getAccountStatus(), maskEmail(email));
			auditLogService.log(account.getId(), "LOGIN_FAILED_INACTIVE_ACCOUNT", servletRequest);
			throw new AccountInactiveException();
		}

		String currentUserAgent = servletRequest.getHeader("User-Agent");
		if (currentUserAgent != null && !currentUserAgent.isBlank()) {
			List<com.project.authservice.entity.UserSession> sameDeviceSessions =
					userSessionRepository.findByAccountIdAndIsOnlineTrueAndUserAgent(account.getId(), currentUserAgent);
			sameDeviceSessions.forEach(credentialRevocationService::revoke);
		}

		redisTemplate.delete(loginAttemptKey(email));
		account.setLastLoginAt(LocalDateTime.now());
		accountRepository.save(account);
		com.project.authservice.entity.LoginHistory loginHistory = com.project.authservice.entity.LoginHistory.builder()
				.account(account)
				.ipAddress(servletRequest.getRemoteAddr())
				.userAgent(currentUserAgent)
				.status("SUCCESS")
				.build();
		loginHistoryRepository.save(loginHistory);

		auditLogService.log(account.getId(), "LOGIN_SUCCESS", servletRequest);
		log.info("User {} logged in successfully", maskEmail(email));
		return issueTokens(account, servletRequest, request.isRememberMe() ? 30 : 7);
	}

	private ValidationResult waitForValidation(
			String requestId, CompletableFuture<ValidationResult> localFuture) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			try {
				return localFuture.get(250, TimeUnit.MILLISECONDS);
			} catch (TimeoutException ignored) {
				String stored = redisTemplate.opsForValue()
						.get("registration_validation_result:" + requestId);
				if (stored != null) {
					return objectMapper.readValue(stored, ValidationResult.class);
				}
			}
		}
		throw new TimeoutException("Registration validation timed out");
	}

	@Override
	@Transactional
	public JwtResponse loginOAuth2(Account account, HttpServletRequest request) {
		String email = account.getEmail();

		com.project.authservice.entity.LoginHistory loginHistory = com.project.authservice.entity.LoginHistory.builder()
				.account(account)
				.ipAddress(request.getRemoteAddr())
				.userAgent(request.getHeader("User-Agent"))
				.status("SUCCESS")
				.build();
		loginHistoryRepository.save(loginHistory);

		auditLogService.log(account.getId(), "OAUTH2_LOGIN_SUCCESS", request);
		log.info("User {} logged in via OAuth2 successfully", maskEmail(email));
		return issueTokens(account, request, 30);
	}

	/**
	 * Refreshes an access token using a refresh token.
	 *
	 * @param request refresh request
	 * @return new jwt response with rotated tokens
	 */
	@Override
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public JwtResponse refreshToken(RefreshTokenRequest request) {
		String tokenStr = request.getRefreshToken();
		String hashedToken = RefreshTokenHashUtil.hash(tokenStr);
		try {
			RefreshToken refreshToken = refreshTokenRepository.findByTokenForUpdate(hashedToken)
					.orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

			if (refreshToken.getIsRevoked() == null || refreshToken.getIsRevoked()) {
				if (refreshToken.getAccount() != null) {
					credentialRevocationService.revokeAll(refreshToken.getAccount().getId());
				}
				throw new InvalidRefreshTokenException("Refresh token is revoked");
			}

			if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
				refreshToken.setIsRevoked(true);
				refreshToken.setRevokedAt(LocalDateTime.now());
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
			if (!Boolean.TRUE.equals(account.getIsEnabled()) || Boolean.TRUE.equals(account.getIsDeleted())) {
				throw new AccountInactiveException();
			}

			LocalDateTime now = LocalDateTime.now();
			com.project.authservice.entity.UserSession session = userSessionRepository
					.findByRefreshTokenId(refreshToken.getId())
					.orElseThrow(() -> new InvalidRefreshTokenException("Session not found"));
			if (!Boolean.TRUE.equals(session.getIsOnline())
					|| session.getExpiredAt() == null
					|| session.getExpiredAt().isBefore(now)) {
				refreshToken.setIsRevoked(true);
				refreshToken.setRevokedAt(now);
				throw new InvalidRefreshTokenException("Session has expired");
			}

			long originalDays = Math.max(1,
					Duration.between(refreshToken.getCreatedAt(), refreshToken.getExpiryDate()).toDays());
			int refreshDays = originalDays > 7 ? 30 : 7;
			refreshToken.setIsRevoked(true);
			refreshToken.setRevokedAt(now);

			String responseRefreshToken = generateRefreshToken();
			RefreshToken newRefreshToken = createRefreshToken(account, responseRefreshToken,
					now.plusDays(refreshDays), refreshToken.getDeviceId());
			session.setRefreshToken(newRefreshToken);
			session.setLastActiveAt(now);
			session.setExpiredAt(newRefreshToken.getExpiryDate());
			userSessionRepository.save(session);

			Role role = requirePrimaryRole(account);
			String newAccessToken = jwtUtil.generateToken(account.getId(), account.getEmail(),
					role.getCode(), permissionCodes(role), session.getId(),
					account.getStatus() == com.project.authservice.enums.AccountStatus.ACTIVE);
			auditLogService.log(account.getId(), "REFRESH_TOKEN_SUCCESS", servletRequest);

			long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
			return new JwtResponse(
					newAccessToken,
					responseRefreshToken,
					expiresInSeconds,
					account.getEmail(),
					role.getCode(),
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
		log.info("Logout request for email={}", maskEmail(email));

		long exp = jwtUtil.extractExpiration(token).getTime();
		long now = System.currentTimeMillis();
		String tokenHash = RefreshTokenHashUtil.hash(token);
		if (exp > now) {
			redisTemplate.opsForValue().set("blacklist:" + tokenHash, "revoked", Duration.ofMillis(exp - now));
		}

		Account account = accountRepository.findByEmail(email).orElse(null);
		if (account != null) {
			Long sessionId = jwtUtil.extractSessionId(token);
			if (sessionId != null) {
				userSessionRepository.findByIdAndAccountId(sessionId, account.getId())
						.ifPresent(credentialRevocationService::revoke);
			}
			auditLogService.log(account.getId(), "LOGOUT_SUCCESS", servletRequest);
		}
	}

	@Override
	@Transactional
	public void logoutAll(String email) {
		log.info("LogoutAll request for email={}", maskEmail(email));
		Account account = accountRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
			
		credentialRevocationService.revokeAll(account.getId());
		auditLogService.log(account.getId(), "LOGOUT_ALL_SUCCESS", servletRequest);
	}
	@Override
	@Transactional
	public void forgotPassword(com.project.authservice.dto.request.ForgotPasswordRequest request) {
		String email = normalizeEmail(request.getEmail());
		log.info("Forgot password requested for email={}", maskEmail(email));
		Account account = accountRepository.findByEmail(email).orElse(null);
		if (account == null
				|| account.getAccountStatus() != com.project.authservice.enums.AccountStatus.ACTIVE
				|| !Boolean.TRUE.equals(account.getIsEnabled())
				|| Boolean.TRUE.equals(account.getIsDeleted())) {
			return;
		}
		passwordResetTokenRepository.findByAccountIdAndIsUsedFalse(account.getId()).forEach(existing -> {
			existing.setIsUsed(true);
			existing.setUsedAt(LocalDateTime.now());
		});

		String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
		com.project.authservice.entity.PasswordResetToken resetToken = com.project.authservice.entity.PasswordResetToken.builder()
				.account(account)
				.otpCode(otp)
				.expiredAt(LocalDateTime.now().plusMinutes(15))
				.isUsed(false)
				.attempts(0)
				.build();
		passwordResetTokenRepository.save(resetToken);
		
		verificationService.sendForgotPasswordEmail(account.getId(), email, otp);
		log.info("Password reset email requested for accountId={}", account.getId());
	}

	@Override
	@Transactional
	public void resetPassword(com.project.authservice.dto.request.ResetPasswordRequest request) {
		log.info("Reset password requested");
		List<com.project.authservice.entity.PasswordResetToken> candidates =
				passwordResetTokenRepository.findAllByOtpCodeAndIsUsedFalseOrderByCreatedAtDesc(request.getToken());
		String email = normalizeEmail(request.getEmail());
		if (email == null && candidates.size() != 1) {
			throw new com.project.authservice.exception.BusinessException("Invalid or expired reset token");
		}
		com.project.authservice.entity.PasswordResetToken resetToken = candidates.stream()
				.filter(candidate -> email == null || email.equals(candidate.getAccount().getEmail()))
				.findFirst()
				.orElseThrow(() -> new com.project.authservice.exception.BusinessException("Invalid or expired reset token"));

		if (resetToken.getIsUsed() || resetToken.getExpiredAt().isBefore(LocalDateTime.now())) {
			throw new com.project.authservice.exception.BusinessException("Invalid or expired reset token");
		}

		Account account = resetToken.getAccount();
		if (passwordEncoder.matches(request.getNewPassword(), account.getPasswordHash())) {
			throw new com.project.authservice.exception.BusinessException(
					"New password must be different from the current password");
		}
		account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		accountRepository.save(account);
		
		resetToken.setIsUsed(true);
		resetToken.setUsedAt(LocalDateTime.now());
		passwordResetTokenRepository.save(resetToken);
		
		credentialRevocationService.revokeAll(account.getId());
		authOutboxService.record("ACCOUNT_PASSWORD_RESET", account.getId(),
				java.util.Map.of("accountId", account.getId()));
		auditLogService.log(account.getId(), "PASSWORD_RESET_SUCCESS", servletRequest);
	}

	@Override
	@Transactional
	public void changePassword(com.project.authservice.dto.request.ChangePasswordRequest request, String email) {
		log.info("Change password requested for email={}", maskEmail(email));
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
				
		if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
			throw new com.project.authservice.exception.BusinessException("Old password incorrect");
		}
		if (passwordEncoder.matches(request.getNewPassword(), account.getPasswordHash())) {
			throw new com.project.authservice.exception.BusinessException(
					"New password must be different from the current password");
		}
		account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		accountRepository.save(account);

		credentialRevocationService.revokeAll(account.getId());
		authOutboxService.record("ACCOUNT_PASSWORD_CHANGED", account.getId(),
				java.util.Map.of("accountId", account.getId()));
		auditLogService.log(account.getId(), "PASSWORD_CHANGED", servletRequest);
	}

	@Override
	@Transactional
	public void requestChangeEmail(com.project.authservice.dto.request.ChangeEmailRequest request, String currentEmail) {
		String normalizedCurrentEmail = normalizeEmail(currentEmail);
		String newEmail = normalizeEmail(request.getNewEmail());
		Account account = accountRepository.findByEmail(normalizedCurrentEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
		if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
			throw new com.project.authservice.exception.BusinessException("Current password is incorrect");
		}
		if (normalizedCurrentEmail.equals(newEmail)) {
			throw new com.project.authservice.exception.BusinessException("New email must be different");
		}
		if (accountRepository.existsByEmail(newEmail)) {
			throw new EmailAlreadyExistsException();
		}

		String key = "change_email:otp:" + normalizedCurrentEmail;
		String existingJson = redisTemplate.opsForValue().get(key);
		if (existingJson != null) {
			try {
				com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(existingJson);
				long lastSentAt = rootNode.path("lastSentAt").asLong(0);
				if (lastSentAt > 0) {
					long elapsedSeconds = (System.currentTimeMillis() - lastSentAt) / 1000;
					if (elapsedSeconds < 60) {
						throw new com.project.authservice.exception.OtpRateLimitException(60 - elapsedSeconds);
					}
				}
			} catch (Exception e) {
				log.warn("Failed to parse existing change email OTP data", e);
			}
		}

		String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
		String otpHash = passwordEncoder.encode(otp);

		try {
			String json = objectMapper.writeValueAsString(java.util.Map.of(
					"otpHash", otpHash,
					"newEmail", newEmail,
					"failedAttempts", 0,
					"lastSentAt", System.currentTimeMillis()
			));
			redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(5));
		} catch (Exception e) {
			throw new RuntimeException("Internal error saving OTP");
		}

		verificationService.sendChangeEmailOtp(account.getId(), normalizedCurrentEmail, newEmail, otp);
		log.info("Requested change email for accountId={}, OTP sent to {}", account.getId(), maskEmail(normalizedCurrentEmail));
	}

	@Override
	@Transactional
	public void verifyChangeEmail(com.project.authservice.dto.request.VerifyChangeEmailRequest request, String currentEmail) {
		String normalizedCurrentEmail = normalizeEmail(currentEmail);
		Account account = accountRepository.findByEmail(normalizedCurrentEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));

		String key = "change_email:otp:" + normalizedCurrentEmail;
		String json = redisTemplate.opsForValue().get(key);
		if (json == null) {
			throw new com.project.authservice.exception.InvalidOtpException();
		}

		try {
			com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(json);
			String otpHash = rootNode.path("otpHash").asText();
			String newEmail = rootNode.path("newEmail").asText();
			int attempts = rootNode.path("failedAttempts").asInt(0);
			long lastSentAt = rootNode.path("lastSentAt").asLong(0);

			if (!passwordEncoder.matches(request.getOtp(), otpHash)) {
				attempts++;
				if (attempts >= 5) {
					redisTemplate.delete(key);
					throw new com.project.authservice.exception.InvalidOtpException();
				} else {
					String updatedJson = objectMapper.writeValueAsString(java.util.Map.of(
							"otpHash", otpHash,
							"newEmail", newEmail,
							"failedAttempts", attempts,
							"lastSentAt", lastSentAt
					));
					Long expire = redisTemplate.getExpire(key);
					if (expire != null && expire > 0) {
						redisTemplate.opsForValue().set(key, updatedJson, Duration.ofSeconds(expire));
					}
					throw new com.project.authservice.exception.InvalidOtpException();
				}
			}

			if (accountRepository.existsByEmail(newEmail)) {
				throw new EmailAlreadyExistsException();
			}

			account.setEmail(newEmail);
			accountRepository.save(account);
			redisTemplate.delete(key);

			credentialRevocationService.revokeAll(account.getId());
			authOutboxService.record("ACCOUNT_EMAIL_CHANGED", account.getId(),
					java.util.Map.of("accountId", account.getId(), "email", newEmail));
			auditLogService.log(account.getId(), "EMAIL_CHANGED", servletRequest);
			log.info("Email changed successfully for accountId={} to newEmail={}", account.getId(), maskEmail(newEmail));

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new RuntimeException("Internal error verifying OTP");
		}
	}

	private JwtResponse issueTokens(Account account, HttpServletRequest request, int refreshDays) {
		Role role = requirePrimaryRole(account);
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshDays);
		String plainRefreshToken = generateRefreshToken();
		String userAgent = truncate(request.getHeader("User-Agent"), 500);
		RefreshToken refreshToken = createRefreshToken(account, plainRefreshToken, expiresAt,
				RefreshTokenHashUtil.hash((userAgent == null ? "unknown" : userAgent) + ":" + account.getId()));

		com.project.authservice.entity.UserSession session = com.project.authservice.entity.UserSession.builder()
				.account(account)
				.refreshToken(refreshToken)
				.deviceName(deviceName(userAgent))
				.deviceType(deviceType(userAgent))
				.browser(browser(userAgent))
				.operatingSystem(operatingSystem(userAgent))
				.ipAddress(truncate(request.getRemoteAddr(), 45))
				.userAgent(userAgent)
				.expiresAt(expiresAt)
				.isActive(true)
				.build();
		session = userSessionRepository.save(session);
		String accessToken = jwtUtil.generateToken(account.getId(), account.getEmail(), role.getCode(),
				permissionCodes(role), session.getId(),
				account.getStatus() == com.project.authservice.enums.AccountStatus.ACTIVE);
		return new JwtResponse(accessToken, plainRefreshToken, jwtUtil.getJwtExpirationMs() / 1000L,
				account.getEmail(), role.getCode(), account.getId());
	}

	private RefreshToken createRefreshToken(Account account, String plainToken,
			LocalDateTime expiresAt, String deviceId) {
		RefreshToken token = new RefreshToken();
		token.setAccount(account);
		token.setToken(RefreshTokenHashUtil.hash(plainToken));
		token.setDeviceId(truncate(deviceId, 120));
		token.setExpiryDate(expiresAt);
		token.setIsRevoked(false);
		return refreshTokenRepository.save(token);
	}

	private String generateRefreshToken() {
		return UUID.randomUUID() + "." + UUID.randomUUID();
	}

	private Role requirePrimaryRole(Account account) {
		Role role = account.getRole();
		if (role == null) {
			throw new IllegalStateException("Account has no assigned role");
		}
		return role;
	}

	private Set<String> permissionCodes(Role role) {
		return role.getPermissions().stream()
				.map(com.project.authservice.entity.Permission::getCode)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private void enforceLoginRateLimit(String email) {
		String attempts = redisTemplate.opsForValue().get(loginAttemptKey(email));
		if (attempts != null && Long.parseLong(attempts) >= 5) {
			throw new com.project.authservice.exception.LoginRateLimitException();
		}
	}

	private void recordFailedLogin(String email) {
		String key = loginAttemptKey(email);
		Long attempts = redisTemplate.opsForValue().increment(key);
		if (attempts != null && attempts == 1L) {
			redisTemplate.expire(key, Duration.ofMinutes(30));
		}
	}

	private String loginAttemptKey(String email) {
		return "login_attempt:" + RefreshTokenHashUtil.hash(email);
	}

	private String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private String truncate(String value, int maxLength) {
		return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private String deviceName(String userAgent) {
		return truncate(userAgent == null || userAgent.isBlank() ? "Unknown device" : userAgent, 150);
	}

	private String deviceType(String userAgent) {
		String value = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
		return value.contains("mobile") || value.contains("android") || value.contains("iphone")
				? "MOBILE" : "DESKTOP";
	}

	private String browser(String userAgent) {
		String value = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
		if (value.contains("edg/")) return "Edge";
		if (value.contains("chrome/")) return "Chrome";
		if (value.contains("firefox/")) return "Firefox";
		if (value.contains("safari/")) return "Safari";
		return "Unknown";
	}

	private String operatingSystem(String userAgent) {
		String value = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
		if (value.contains("windows")) return "Windows";
		if (value.contains("android")) return "Android";
		if (value.contains("iphone") || value.contains("ipad")) return "iOS";
		if (value.contains("mac os")) return "macOS";
		if (value.contains("linux")) return "Linux";
		return "Unknown";
	}
}
