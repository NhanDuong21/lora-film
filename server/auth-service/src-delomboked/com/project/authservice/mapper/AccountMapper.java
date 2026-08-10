package com.project.authservice.mapper;

import org.springframework.stereotype.Component;

import com.project.authservice.dto.response.RegisterResponse;
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
		response.setAccountId(account.getId());
		response.setEmail(account.getEmail());
		response.setRole(account.getRole() != null ? account.getRole().getRoleName() : null);
		return response;
	}

}