package com.project.authservice.service.impl;

import com.project.authservice.dto.SessionDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.UserSession;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;
    private final AccountRepository accountRepository;
    private final com.project.authservice.service.CredentialRevocationService credentialRevocationService;

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessions(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        return userSessionRepository.findByAccountIdAndIsOnlineTrue(account.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId, String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        UserSession session = userSessionRepository.findByIdAndAccountId(sessionId, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found or belongs to another user"));
        
        credentialRevocationService.revoke(session);
    }

    @Override
    @Transactional
    public void revokeAllSessions(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        credentialRevocationService.revokeAll(account.getId());
    }

    private SessionDto mapToDto(UserSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .deviceName(session.getDeviceName())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .lastActiveAt(session.getLastActiveAt())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .build();
    }
    public SessionServiceImpl(UserSessionRepository userSessionRepository, AccountRepository accountRepository,
                              com.project.authservice.service.CredentialRevocationService credentialRevocationService) {
        this.userSessionRepository = userSessionRepository;
        this.accountRepository = accountRepository;
        this.credentialRevocationService = credentialRevocationService;
    }
}
