package com.project.authservice.util;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration}")
	private int jwtExpirationMs;

	public String generateToken(Long userId, String email, String role) {
		return generateToken(userId, email, role, Set.of(), null);
	}

	public String generateToken(Long userId, String email, String role,
			Set<String> permissions, Long sessionId) {
		return generateToken(userId, email, role, permissions, sessionId, false);
	}

	public String generateToken(Long userId, String email, String role,
			Set<String> permissions, Long sessionId, boolean identityVerified) {
		long issuedAtMs = System.currentTimeMillis();
		Date issuedAt = new Date(issuedAtMs);
		return Jwts.builder()
				.subject(email)
				.claim("userId", userId)
				.claim("role", role)
				.claim("permissions", permissions == null ? Set.of() : permissions)
				.claim("sid", sessionId)
				.claim("identityVerified", identityVerified)
				.claim("tokenType", "access")
				.claim("iatMs", issuedAtMs)
				.id(UUID.randomUUID().toString())
				.issuedAt(issuedAt)
				.expiration(new Date(issuedAt.getTime() + jwtExpirationMs))
				.signWith(key())
				.compact();
	}

	private SecretKey key() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
	}

	public int getJwtExpirationMs() {
		return jwtExpirationMs;
	}

	public String extractUsername(String token) {
		return extractClaims(token).getSubject();
	}

	public String extractRole(String token) {
		return extractClaims(token).get("role", String.class);
	}

	public Date extractExpiration(String token) {
		return extractClaims(token).getExpiration();
	}

	public Long extractUserId(String token) {
		return extractClaims(token).get("userId", Long.class);
	}

	public Long extractSessionId(String token) {
		Number value = extractClaims(token).get("sid", Number.class);
		return value == null ? null : value.longValue();
	}

	public Date extractIssuedAt(String token) {
		return extractClaims(token).getIssuedAt();
	}

	public long extractIssuedAtMillis(String token) {
		Claims claims = extractClaims(token);
		Number issuedAtMs = claims.get("iatMs", Number.class);
		if (issuedAtMs != null) {
			return issuedAtMs.longValue();
		}
		Date issuedAt = claims.getIssuedAt();
		return issuedAt == null ? Long.MIN_VALUE : issuedAt.getTime();
	}

	public String extractTokenType(String token) {
		return extractClaims(token).get("tokenType", String.class);
	}

	public Set<String> extractPermissions(String token) {
		Object value = extractClaims(token).get("permissions");
		if (!(value instanceof List<?> list)) {
			return Set.of();
		}
		return list.stream().filter(String.class::isInstance).map(String.class::cast)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	public Claims extractClaims(String token) {
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
	}

	public boolean validateToken(String token) {
		try {
			Claims claims = extractClaims(token);
			return "access".equals(claims.get("tokenType", String.class))
					&& claims.getSubject() != null
					&& claims.get("userId") != null;
		} catch (Exception e) {
			return false;
		}
	}
}
