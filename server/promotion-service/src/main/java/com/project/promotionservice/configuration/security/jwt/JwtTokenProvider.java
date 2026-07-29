package com.project.promotionservice.configuration.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("CRITICAL SECURITY FAILURE: 'jwt.secret' configuration is missing! Must configure JWT_SECRET environment variable.");
        }
        try {
            if (getSigningKey().getEncoded().length < 32) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY FAILURE: JWT signing key must be at least 256 bits");
            }
        } catch (RuntimeException invalidKey) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY FAILURE: 'jwt.secret' is not a valid HS256-or-stronger key",
                    invalidKey);
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            Object userId = claims.get("userId");
            return "access".equals(claims.get("tokenType", String.class))
                    && claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && positiveUserId(userId);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private boolean positiveUserId(Object value) {
        if (value instanceof Number number) {
            return number.longValue() > 0;
        }
        if (value == null) {
            return false;
        }
        try {
            return Long.parseLong(value.toString()) > 0;
        } catch (NumberFormatException invalidUserId) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
