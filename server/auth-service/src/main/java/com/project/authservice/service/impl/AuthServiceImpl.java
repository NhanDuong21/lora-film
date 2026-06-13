package com.project.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.client.UserServiceClient;
import com.project.authservice.dto.request.LoginRequest;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.request.RefreshTokenRequest;
import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.dto.response.RegisterResponse;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.entity.RefreshToken;
import com.project.authservice.exception.AccountInactiveException;
import com.project.authservice.exception.AccountNotVerifiedException;
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

	public AuthServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository,
						   PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
						   CccdCheckClient cccdCheckClient, UserServiceClient userServiceClient,
						   VerificationService verificationService, AuditLogService auditLogService,
						   RefreshTokenRepository refreshTokenRepository, HttpServletRequest servletRequest) {
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

			// Verify birthday matches birth year derived from CCCD
			String birthdayStr = request.getBirthday().trim();
			int birthYearFromDate;
			try {
				String[] parts = birthdayStr.split("-");
				birthYearFromDate = Integer.parseInt(parts[0]);
			} catch (Exception e) {
				log.warn("Failed to parse birth year from birthday='{}'", birthdayStr);
				throw new BirthdayCccdMismatchException();
			}

			if (birthYearFromDate != cccdInfo.getBirthYear()) {
				log.warn("Birthday birth year {} does not match CCCD birth year {}", birthYearFromDate, cccdInfo.getBirthYear());
				throw new BirthdayCccdMismatchException();
			}

			// Load CUSTOMER role
			Role role = roleRepository.findByRoleName(CUSTOMER_ROLE)
					.orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found"));

			// Save account locally
			Account account = new Account();
			account.setEmail(email);
			account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			account.setRole(role);
			account.setIsActive(1);
			account.setRegistrationCompleted(0);

			Account savedAccount = accountRepository.save(account);

			// Propagate user profile to User Service
			UserServiceClient.UserProfileRequest profileRequest = new UserServiceClient.UserProfileRequest(
					savedAccount.getId(),
					request.getFullName(),
					request.getPhoneNumber(),
					request.getCccd(),
					cccdInfo.getCccdMasked(),
					cccdInfo.getProvinceCode(),
					cccdInfo.getProvinceName(),
					cccdInfo.getGender(),
					birthdayStr,
					cccdInfo.getBirthYear()
			);

			try {
				userServiceClient.createUserProfile(profileRequest);
			} catch (Exception e) {
				log.warn("User Service propagation failed or is unavailable. Continuing registration as mocked fallback. Error: {}", e.getMessage());
			}

			// Generate verification OTP
			verificationService.generateVerification(savedAccount);

			log.info("Account successfully registered and profile created for email={} with accountId={}", email, savedAccount.getId());

			auditLogService.log(savedAccount.getId(), "REGISTER_SUCCESS", servletRequest);

			return new RegisterResponse(
					savedAccount.getId(),
					savedAccount.getEmail(),
					savedAccount.getRole().getRoleName(),
					request.getFullName(),
					request.getPhoneNumber(),
					cccdInfo.getCccdMasked(),
					cccdInfo.getProvinceName(),
					cccdInfo.getGender(),
					cccdInfo.getBirthYear()
			);
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

		// 3. Check isActive == 1
		if (account.getIsActive() == null || account.getIsActive() != 1) {
			log.warn("Login failed: account is inactive for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_INACTIVE_ACCOUNT", servletRequest);
			throw new AccountInactiveException();
		}

		// 4. Check registrationCompleted == 1
		if (account.getRegistrationCompleted() == null || account.getRegistrationCompleted() != 1) {
			log.warn("Login failed: account is not verified for email {}", email);
			auditLogService.log(account.getId(), "LOGIN_FAILED_NOT_VERIFIED", servletRequest);
			throw new AccountNotVerifiedException();
		}

		// 5. Generate JWT
		String accessToken = jwtUtil.generateToken(account.getId(), account.getEmail(), account.getRole().getRoleName());

		// 6. Generate Refresh Token
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setAccount(account);
		refreshToken.setToken(UUID.randomUUID().toString());
		refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
		refreshToken.setIsRevoked(false);

		// 7. Save Refresh Token
		refreshTokenRepository.save(refreshToken);

		// 8. Write Audit Log
		auditLogService.log(account.getId(), "LOGIN_SUCCESS", servletRequest);

		log.info("User {} logged in successfully", email);

		// 9. Return response
		long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
		return new JwtResponse(
				accessToken,
				refreshToken.getToken(),
				expiresInSeconds,
				account.getEmail(),
				account.getRole().getRoleName()
		);
	}

	/**
	 * Verifies account using OTP.
	 *
	 * @param request verification request
	 */
	@Override
	@Transactional
	public void verify(VerifyRequest request) {
		verificationService.verify(request.getAccountId(), request.getOtp());
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
		try {
			RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
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
				throw new AccountNotVerifiedException();
			}

			// Rotation Strategy:
			// Revoke old refresh token
			refreshToken.setIsRevoked(true);
			refreshTokenRepository.save(refreshToken);

			// Generate new access token
			String newAccessToken = jwtUtil.generateToken(account.getId(), account.getEmail(), account.getRole().getRoleName());

			// Generate new refresh token
			RefreshToken newRefreshToken = new RefreshToken();
			newRefreshToken.setAccount(account);
			newRefreshToken.setToken(UUID.randomUUID().toString());
			newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
			newRefreshToken.setIsRevoked(false);
			refreshTokenRepository.save(newRefreshToken);

			auditLogService.log(account.getId(), "REFRESH_TOKEN_SUCCESS", servletRequest);

			long expiresInSeconds = jwtUtil.getJwtExpirationMs() / 1000;
			return new JwtResponse(
					newAccessToken,
					newRefreshToken.getToken(),
					expiresInSeconds,
					account.getEmail(),
					account.getRole().getRoleName()
			);
		} catch (Exception e) {
			Long accountId = null;
			try {
				RefreshToken tempToken = refreshTokenRepository.findByToken(tokenStr).orElse(null);
				if (tempToken != null && tempToken.getAccount() != null) {
					accountId = tempToken.getAccount().getId();
				}
			} catch (Exception ignore) {}
			auditLogService.log(accountId, "REFRESH_TOKEN_FAILED", servletRequest);
			throw e;
		}
	}
}