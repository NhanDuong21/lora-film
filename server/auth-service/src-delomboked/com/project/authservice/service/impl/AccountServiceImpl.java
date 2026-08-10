package com.project.authservice.service.impl;

import com.project.authservice.dto.AccountDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.Role;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.AccountService;
import com.project.authservice.service.AuditLogService;
import com.project.authservice.service.AuthOutboxService;
import com.project.authservice.service.CredentialRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest servletRequest;
    private final AuthOutboxService outboxService;
    private final CredentialRevocationService revocationService;

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
        if (status == null) {
            throw new BusinessException("Account status is required");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        AccountStatus previous = account.getAccountStatus();
        if (previous == status) {
            throw new BusinessException("Account already has status " + status);
        }
        account.setAccountStatus(status);
        account = accountRepository.save(account);
        
        if (status == AccountStatus.BLOCKED || status == AccountStatus.INACTIVE || status == AccountStatus.SUSPENDED) {
            revocationService.revokeAll(account.getId());
        }
        String eventType = status == AccountStatus.ACTIVE && previous == AccountStatus.BLOCKED
                ? "ACCOUNT_UNLOCKED"
                : status == AccountStatus.BLOCKED ? "ACCOUNT_LOCKED" : "ACCOUNT_STATUS_CHANGED";
        outboxService.record(eventType, account.getId(), Map.of(
                "accountId", account.getId(),
                "previousStatus", previous.name(),
                "status", status.name()));
        auditLogService.log(account.getId(), "UPDATE_ACCOUNT_STATUS", servletRequest);
        return mapToDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccountRole(Long id, Integer roleId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
                
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Role previousRole = account.getRole();
        if (previousRole.getId().equals(role.getId())) {
            throw new BusinessException("Role " + role.getRoleName() + " is already assigned");
        }
        account.setRole(role);
        account = accountRepository.save(account);

        revocationService.revokeAll(account.getId());
        outboxService.record("ROLE_REMOVED", "ACCOUNT", account.getId(),
                Map.of("accountId", account.getId(), "role", previousRole.getRoleName()));
        outboxService.record("ROLE_ASSIGNED", "ACCOUNT", account.getId(),
                Map.of("accountId", account.getId(), "role", role.getRoleName()));
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
    public AccountServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository, AuditLogService auditLogService, HttpServletRequest servletRequest, AuthOutboxService outboxService, CredentialRevocationService revocationService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.servletRequest = servletRequest;
        this.outboxService = outboxService;
        this.revocationService = revocationService;
    }
}
