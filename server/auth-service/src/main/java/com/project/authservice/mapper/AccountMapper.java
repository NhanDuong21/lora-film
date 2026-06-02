package com.project.authservice.mapper;

import org.springframework.stereotype.Component;

import com.project.authservice.dto.RegisterResponse;
import com.project.authservice.entity.Account;

@Component
public class AccountMapper {
	/**
	 * Maps an account entity to the register response payload.
	 *
	 * @param account persisted account entity
	 * @return register response
	 */
	public RegisterResponse toRegisterResponse(Account account) {
		RegisterResponse response = new RegisterResponse();
		response.setId(account.getId());
		response.setEmail(account.getEmail());
		response.setRole(account.getRole() != null ? account.getRole().getName() : null);
		response.setStatus(account.getStatus() != null ? account.getStatus().name() : null);
		return response;
	}
}