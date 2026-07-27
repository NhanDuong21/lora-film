package com.project.authservice.service.impl;

import com.project.authservice.dto.SessionDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.UserSession;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.SessionService;
import com.project.authservice.service.CredentialRevocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;
    private final AccountRepository accountRepository;
    private final CredentialRevocationService revocationService;

    @Override
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessions(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        return userSessionRepository.findByAccountIdAndIsActiveTrue(account.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId, String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        UserSession session = userSessionRepository.findByIdAndAccountId(sessionId, account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found or belongs to another user"));
        
        revocationService.revoke(session);
    }

    @Override
    @Transactional
    public void revokeAllSessions(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        revocationService.revokeAll(account.getId());
    }

    private SessionDto mapToDto(UserSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .deviceName(session.getDeviceName())
                .lastActiveAt(session.getLastActiveAt())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .build();
    }
    public SessionServiceImpl(UserSessionRepository userSessionRepository, AccountRepository accountRepository, CredentialRevocationService revocationService) {
        this.userSessionRepository = userSessionRepository;
        this.accountRepository = accountRepository;
        this.revocationService = revocationService;
    }
}
