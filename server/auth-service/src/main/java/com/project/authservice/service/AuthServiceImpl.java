package com.project.authservice.service;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.dto.RegisterRequest;
import com.project.authservice.dto.RegisterResponse;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
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
	private static final String CUSTOMER_ROLE = "CUSTOMER";

	private final AccountRepository accountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccountMapper accountMapper;

	/**
	 * Registers a new user account.
	 *
	 * @param request registration request
	 * @return register response
	 */
	@Override
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		log.info("Register request received for email={}", email);

		if (accountRepository.existsByEmail(email)) {
			throw new BusinessException("Email already registered");
		}

		Role role = roleRepository.findByRoleName(CUSTOMER_ROLE)
				.orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found"));

		Account account = Account.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.role(role)
				.isActive(true)
				.build();

		Account savedAccount = accountRepository.save(account);
		RegisterResponse response = accountMapper.toRegisterResponse(savedAccount);

		log.info("Account registered successfully for email={} with id={}", email, Objects.requireNonNull(response.getId()));
		return response;
	}
}