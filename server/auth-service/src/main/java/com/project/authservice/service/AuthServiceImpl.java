package com.project.authservice.service;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.dto.RegisterRequest;
import com.project.authservice.dto.RegisterResponse;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enumtype.AccountStatus;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.mapper.AccountMapper;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private static final String USER_ROLE = "USER";

	private final AccountRepository accountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccountMapper accountMapper;
    private final RegistrationIntegrationService registrationIntegrationService;

	/**
	 * Registers a new user account.
	 *
	 * @param request registration request
	 * @return register response
	 */
	@Override
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String email = request.getEmail().trim();
		log.info("Register request received for email={}", email);

		if (accountRepository.existsByEmail(email)) {
			throw new BusinessException("Email already registered");
		}

		Role role = roleRepository.findByName(USER_ROLE)
				.orElseThrow(() -> new ResourceNotFoundException("Role USER not found"));

		Account account = Account.builder()
				.email(email)
				.password(passwordEncoder.encode(request.getPassword()))
				.role(role)
				.status(AccountStatus.ACTIVE)
				.build();

		Account savedAccount = accountRepository.save(account);
		RegisterResponse response = accountMapper.toRegisterResponse(savedAccount);

		// Forward additional profile fields to integration service for other services to consume
		try {
			registrationIntegrationService.forwardProfileData(
					savedAccount.getId(),
					request.getFullName(),
					request.getCitizenId(),
					request.getGender(),
					request.getDob());
		} catch (Exception ex) {
			log.warn("Failed to forward profile data for accountId={}", savedAccount.getId(), ex);
		}

		log.info("Account registered successfully for email={} with id={}", email, Objects.requireNonNull(response.getId()));
		return response;
	}
}