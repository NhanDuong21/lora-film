package com.project.authservice.service;

import com.project.authservice.dto.AccountDto;
import com.project.authservice.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {
    Page<AccountDto> getAccounts(Pageable pageable);
    AccountDto getAccountById(Long id);
    AccountDto getAccountByEmail(String email);
    AccountDto updateAccountStatus(Long id, AccountStatus status);
    AccountDto updateAccountRole(Long id, Integer roleId);
}
