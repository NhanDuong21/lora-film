package com.project.authservice.service.impl;

import com.project.authservice.dto.AccountDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.AccountService;
import com.project.authservice.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest servletRequest;
    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDto> getAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return mapToDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto getAccountByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        account.setAccountStatus(status);
        account = accountRepository.save(account);
        
        // If locked/inactive, revoke all sessions
        if (status == AccountStatus.LOCKED || status == AccountStatus.INACTIVE) {
            userSessionRepository.revokeAllForAccount(account.getId());
        }
        
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_STATUS", servletRequest);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountRole(Long id, Integer roleId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
                
        Role role = roleRepository.findById(roleId.longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                
        if (account.getRole() != null && account.getRole().getId().equals(role.getId())) {
            throw new com.project.authservice.exception.BusinessException("Role is already assigned to this account");
        }
                
        account.setRole(role);
        account = accountRepository.save(account);
        
        userSessionRepository.revokeAllForAccount(account.getId()); // Revoke sessions to force re-login with new role
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_ROLE", servletRequest);
        return mapToDto(account);
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .roleName(account.getRole().getRoleName())
                .status(account.getAccountStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
    public AccountServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository, AuditLogService auditLogService, HttpServletRequest servletRequest, UserSessionRepository userSessionRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.servletRequest = servletRequest;
        this.userSessionRepository = userSessionRepository;
    }
}
