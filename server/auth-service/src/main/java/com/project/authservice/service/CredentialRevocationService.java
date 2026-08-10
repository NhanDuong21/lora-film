package com.project.authservice.service;

import com.project.authservice.entity.UserSession;
import com.project.authservice.repository.RefreshTokenRepository;
import com.project.authservice.repository.UserSessionRepository;
import com.project.authservice.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class CredentialRevocationService {
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public CredentialRevocationService(UserSessionRepository sessionRepository,
                                       RefreshTokenRepository refreshTokenRepository,
                                       StringRedisTemplate redisTemplate,
                                       JwtUtil jwtUtil) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(Long accountId) {
        sessionRepository.findByAccountIdAndIsOnlineTrue(accountId).forEach(this::revoke);
        var tokens = refreshTokenRepository.findActiveTokensByAccountId(accountId);
        LocalDateTime now = LocalDateTime.now();
        tokens.forEach(token -> {
            token.setIsRevoked(true);
            token.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(tokens);
        redisTemplate.opsForValue().set(
                "account_revoked_after:" + accountId,
                Long.toString(System.currentTimeMillis()),
                Duration.ofMillis(jwtUtil.getJwtExpirationMs()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revoke(UserSession session) {
        if (session.getId() != null) {
            redisTemplate.opsForValue().set(
                    "revoked_session:" + session.getId(),
                    "revoked",
                    Duration.ofMillis(jwtUtil.getJwtExpirationMs()));
        }
        if (session.getRefreshToken() != null && !Boolean.TRUE.equals(session.getRefreshToken().getIsRevoked())) {
            session.getRefreshToken().setIsRevoked(true);
            session.getRefreshToken().setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(session.getRefreshToken());
        }
        session.setIsActive(false);
        session.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(session);
    }
}
