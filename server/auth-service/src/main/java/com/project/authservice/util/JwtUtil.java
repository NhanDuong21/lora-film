package com.project.authservice.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration}")
	private int jwtExpirationMs;

	public String generateToken(Long userId, String email, String role) {
		return Jwts.builder()
				.subject(email)
				.claim("userId", userId)
				.claim("role", role)
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
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject();
	}

	public String extractRole(String token) {
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().get("role", String.class);
	}

	public Date extractExpiration(String token) {
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getExpiration();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
