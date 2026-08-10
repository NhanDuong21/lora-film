package com.project.authservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RefreshTokenHashUtil {

	private RefreshTokenHashUtil() {
	}

	public static String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hashBytes.length * 2);
			for (byte b : hashBytes) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}
}
