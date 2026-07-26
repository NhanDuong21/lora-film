package com.project.authservice.service.impl;

import com.project.authservice.dto.SessionDto;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.UserSession;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;
    private final AccountRepository accountRepository;

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
        
        session.setIsActive(false);
        userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void revokeAllSessions(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        userSessionRepository.revokeAllForAccount(account.getId());
    }

    private SessionDto mapToDto(UserSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .build();
    }
}
