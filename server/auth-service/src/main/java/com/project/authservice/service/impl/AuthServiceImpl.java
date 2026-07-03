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
	
	private final ConcurrentHashMap<String, CompletableFuture<ValidationResult>> pendingRequests = new ConcurrentHashMap<>();

	public AuthServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
			CccdCheckClient cccdCheckClient,
			VerificationService verificationService, AuditLogService auditLogService,
			RefreshTokenRepository refreshTokenRepository, HttpServletRequest servletRequest,
			AuthAccountEventPublisher eventPublisher, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
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
	}

	/**
	 * Registers a new user account.
	 *
	 * @param request registration request
	 * @return register response
	 */
	public void completeValidation(String requestId, ValidationResult result) {
		CompletableFuture<ValidationResult> future = pendingRequests.get(requestId);
		if (future != null) {
			future.complete(result);
		}
	}

	@Override
	public RegistrationInitiatedResponse register(RegisterRequest request) {
		try {
			String email = request.getEmail().trim().toLowerCase();
			log.info("Register request received for email={}", email);

			Account existingAccount = accountRepository.findByEmail(email).orElse(null);
			if (existingAccount != null) {
				if (!"PENDING".equals(existingAccount.getAccountStatus())) {
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

					Account account = existingAccount != null ? existingAccount : new Account();
					account.setEmail(email);
					account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
					account.setRole(role);
					account.setAccountStatus("PENDING");
					accountRepository.save(account);

					String pendingKey = "pending_registration:" + email;
					redisTemplate.opsForValue().set(pendingKey, json, Duration.ofMinutes(15));
					verificationService.sendOtp(new SendOtpRequest(email));
					redisTemplate.delete("temp_request:" + requestId);
					
					return new RegistrationInitiatedResponse(requestId, "Registration successful, please check your email for OTP");
				} else {
					redisTemplate.delete("temp_request:" + requestId);
                    
                    String message = "Registration information (Phone number or CCCD) already exists.";
                    if ("PHONE_NUMBER_RESERVED".equals(result.getErrorCode())) {
                        message = "Phone number is currently reserved by another pending registration. Please try again later.";
                    } else if ("CCCD_RESERVED".equals(result.getErrorCode())) {
                        message = "CCCD is currently reserved by another pending registration. Please try again later.";
                    } else if ("PHONE_NUMBER_ALREADY_EXISTS".equals(result.getErrorCode())) {
                        message = "Phone number already exists.";
                    } else if ("CCCD_ALREADY_EXISTS".equals(result.getErrorCode())) {
                        message = "CCCD already exists.";
                    }
                    
                    if (result.getRetryAfterSeconds() != null) {
                        throw new RegistrationConflictException(message, result.getErrorCode(), result.getRetryAfterSeconds());
                    }
                    // For already exists, use DuplicateResourceException mapped to BUSINESS_ERROR previously, 
                    // but since the requirement says "Replace the generic error code with the following specific error codes",
                    // we will throw RegistrationConflictException for all to carry the correct errorCode.
					throw new RegistrationConflictException(message, result.getErrorCode() != null ? result.getErrorCode() : "BUSINESS_ERROR", null);
				}
			} catch (TimeoutException e) {
				redisTemplate.delete("temp_request:" + requestId);
				throw new RuntimeException("Request timeout waiting for validation", e);
			} catch (RegistrationConflictException | RegistrationAlreadyPendingException e) {
				throw e;
			} catch (DuplicateResourceException e) {
				throw e;
			} catch (Exception e) {
				redisTemplate.delete("temp_request:" + requestId);
				throw new RuntimeException("Internal error processing registration", e);
			} finally {
				pendingRequests.remove(requestId);
			}
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

		savedAccount.setAccountStatus("ACTIVE");
		accountRepository.save(savedAccount);

		log.info("Account verified successfully for email={} with accountId={}", email, savedAccount.getId());
		auditLogService.log(savedAccount.getId(), "REGISTER_SUCCESS", servletRequest);

		try {
			eventPublisher.publishAccountVerified(savedAccount, registerRequest, cccdInfo);
		} catch (Exception kafkaEx) {
			log.error("ACCOUNT_VERIFIED Kafka event failed for accountId={} email={}: {}",
					savedAccount.getId(), email, kafkaEx.getMessage(), kafkaEx);
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
		if ("PENDING".equals(account.getAccountStatus())) {
			log.warn("Login failed: account is not verified for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_NOT_VERIFIED", servletRequest);
			throw new AccountNotVerifiedException(account.getId());
		}

		// 4. Check if account is active
		if (!"ACTIVE".equals(account.getAccountStatus())) {
			log.warn("Login failed: account is inactive (status={}) for email {}", account.getAccountStatus(), email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_INACTIVE_ACCOUNT", servletRequest);
			throw new AccountInactiveException();
		}

		// 5. Generate JWT
		String accessToken = jwtUtil.generateToken(account.getId(), account.getEmail(),
				account.getRole().getRoleName());

		// 6. Revoke any existing active refresh tokens issued from the same
		// browser/device.
		// Device identity is determined by matching the User-Agent header against the
		// user_agent stored in the audit_logs entry that was written during the
		// original login.
		String currentUserAgent = servletRequest.getHeader("User-Agent");
		if (currentUserAgent != null && !currentUserAgent.isBlank()) {
			List<RefreshToken> sameDeviceTokens = refreshTokenRepository
					.findActiveTokensByAccountAndUserAgent(account.getId(), currentUserAgent);
			if (!sameDeviceTokens.isEmpty()) {
				sameDeviceTokens.forEach(t -> t.setIsRevoked(true));
				refreshTokenRepository.saveAll(sameDeviceTokens);
				log.info("Revoked {} active refresh token(s) for account {} from the same device (User-Agent match)",
						sameDeviceTokens.size(), email);
			}
		}

		// 7. Generate new Refresh Token
		String plainRefreshToken = UUID.randomUUID().toString();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setAccount(account);
		refreshToken.setToken(RefreshTokenHashUtil.hash(plainRefreshToken));
		refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
		refreshToken.setIsRevoked(false);

		// 8. Save new Refresh Token
		refreshTokenRepository.save(refreshToken);

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

			if ("PENDING".equals(account.getAccountStatus())) {
				throw new AccountNotVerifiedException(account.getId());
			}

			if (!"ACTIVE".equals(account.getAccountStatus())) {
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
}