package com.project.authservice.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.client.UserServiceClient;
import com.project.authservice.event.publisher.AuthAccountEventPublisher;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegisterResponse;
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
	private final UserServiceClient userServiceClient;
	private final VerificationService verificationService;
	private final AuditLogService auditLogService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final HttpServletRequest servletRequest;
	private final AuthAccountEventPublisher eventPublisher;

	public AuthServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
			CccdCheckClient cccdCheckClient, UserServiceClient userServiceClient,
			VerificationService verificationService, AuditLogService auditLogService,
			RefreshTokenRepository refreshTokenRepository, HttpServletRequest servletRequest,
			AuthAccountEventPublisher eventPublisher) {
		this.accountRepository = accountRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
		this.cccdCheckClient = cccdCheckClient;
		this.userServiceClient = userServiceClient;
		this.verificationService = verificationService;
		this.auditLogService = auditLogService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.servletRequest = servletRequest;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Registers a new user account.
	 *
	 * @param request registration request
	 * @return register response
	 */
	@Override
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		try {
			String email = request.getEmail().trim().toLowerCase();
			log.info("Register request received for email={}", email);

			if (accountRepository.existsByEmail(email)) {
				log.warn("Email already registered: {}", email);
				throw new EmailAlreadyExistsException();
			}

			// Perform CCCD Check and info derivation
			CccdCheckClient.CccdInfo cccdInfo = cccdCheckClient.checkCccd(request.getCccd());

			// Verify birthday matches birth year derived from CCCD.
			// LocalDate.parse with ISO_LOCAL_DATE enforces strict YYYY-MM-DD and
			// rejects impossible calendar dates (e.g. 2000-02-30).
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

			// Load CUSTOMER role
			Role role = roleRepository.findByRoleName(CUSTOMER_ROLE)
					.orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found"));

			// Save account locally
			Account account = new Account();
			account.setEmail(email);
			account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			account.setRole(role);
			account.setIsActive(0);
			account.setRegistrationCompleted(0);

			Account savedAccount = accountRepository.save(account);
			// DB row is now written; transaction will commit at method exit.
			// Kafka event is published AFTER this save, ensuring no event on DB failure.

			// Generate verification OTP
			verificationService.sendOtp(new com.project.authservice.dto.request.SendOtpRequest(email, "REGISTRATION"));

			log.info("Account successfully registered for email={} with accountId={}", email, savedAccount.getId());

			auditLogService.log(savedAccount.getId(), "REGISTER_SUCCESS", servletRequest);

			// Publish ACCOUNT_CREATED event to Kafka.
			// This runs inside the same @Transactional method; Spring Kafka's
			// KafkaTemplate.send() is called AFTER accountRepository.save() succeeds,
			// so a DB exception will prevent this line from being reached.
			// If Kafka publish fails, we log the error but do NOT rollback the account
			// (idempotent consumer design handles duplicates on retry).
			try {
				eventPublisher.publishAccountCreated(savedAccount, request, cccdInfo);
			} catch (Exception kafkaEx) {
				log.error("ACCOUNT_CREATED Kafka event failed for accountId={} email={}: {}",
						savedAccount.getId(), email, kafkaEx.getMessage(), kafkaEx);
				// Account creation is NOT rolled back – downstream will reconcile via
				// idempotent consumer or a retry/dead-letter mechanism.
			}

			return new RegisterResponse(
					savedAccount.getId(),
					savedAccount.getEmail(),
					savedAccount.getRole().getRoleName(),
					request.getFullName(),
					request.getPhoneNumber(),
					cccdInfo.getCccdMasked(),
					cccdInfo.getProvinceName(),
					cccdInfo.getGender(),
					cccdInfo.getBirthYear());
		} catch (Exception e) {
			auditLogService.log(null, "REGISTER_FAILED", servletRequest);
			throw e;
		}
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

		// 3. Check registrationCompleted == 1
		if (account.getRegistrationCompleted() == null || account.getRegistrationCompleted() != 1) {
			log.warn("Login failed: account is not verified for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_NOT_VERIFIED", servletRequest);
			throw new AccountNotVerifiedException(account.getId());
		}

		// 4. Check isActive == 1 (account must be active to log in)
		if (account.getIsActive() == null || account.getIsActive() != 1) {
			log.warn("Login failed: account is inactive for email {}", email);
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

			if (account.getIsActive() == null || account.getIsActive() != 1) {
				throw new AccountInactiveException();
			}

			if (account.getRegistrationCompleted() == null || account.getRegistrationCompleted() != 1) {
				throw new AccountNotVerifiedException(account.getId());
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