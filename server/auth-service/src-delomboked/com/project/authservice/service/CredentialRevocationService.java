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
        sessionRepository.findByAccountIdAndIsActiveTrue(accountId).forEach(this::revoke);
        var tokens = refreshTokenRepository.findActiveTokensByAccountId(accountId);
        tokens.forEach(token -> token.setIsRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revoke(UserSession session) {
        blacklist(session);
        session.setIsActive(false);
        if (session.getRefreshToken() != null) {
            session.getRefreshToken().setIsRevoked(true);
        }
        sessionRepository.save(session);
    }

    private void blacklist(UserSession session) {
        if (session.getAccessTokenHash() == null || session.getAccessTokenHash().isBlank()) {
            return;
        }
        long sessionRemaining = session.getExpiresAt() == null
                ? jwtUtil.getJwtExpirationMs()
                : Duration.between(LocalDateTime.now(), session.getExpiresAt()).toMillis();
        long ttlMillis = Math.min(jwtUtil.getJwtExpirationMs(), sessionRemaining);
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + session.getAccessTokenHash(),
                    "revoked",
                    Duration.ofMillis(ttlMillis));
        }
    }
}
