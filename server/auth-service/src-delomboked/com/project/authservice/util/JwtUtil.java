package com.project.authservice.util;

import java.util.Collection;
import java.util.Date;
import java.util.List;
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
		return generateToken(userId, email, role, List.of());
	}

	public String generateToken(Long userId, String email, String role, Collection<String> permissions) {
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(email)
				.claim("userId", userId)
				.claim("role", role)
				.claim("permissions", permissions == null ? List.of() : permissions)
				.claim("tokenType", "access")
				.issuedAt(new Date())
				.expiration(new Date((new Date()).getTime() + jwtExpirationMs))
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

	public Long extractUserId(String token) {
		return extractClaims(token).get("userId", Long.class);
	}

	public List<String> extractPermissions(String token) {
		Object value = extractClaims(token).get("permissions");
		if (!(value instanceof Collection<?> collection)) {
			return List.of();
		}
		return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
	}

	public Date extractExpiration(String token) {
		return extractClaims(token).getExpiration();
	}

	public Claims extractClaims(String token) {
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
	}

	public boolean validateToken(String token) {
		try {
			Claims claims = extractClaims(token);
			return claims.getSubject() != null
					&& claims.get("userId") != null
					&& claims.get("role", String.class) != null
					&& "access".equals(claims.get("tokenType", String.class));
		} catch (Exception e) {
			return false;
		}
	}
}
