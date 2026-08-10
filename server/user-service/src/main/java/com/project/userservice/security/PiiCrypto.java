package com.project.userservice.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

public final class PiiCrypto {
    private static final String PREFIX = "enc:v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] KEY = loadKey();

    private PiiCrypto() {
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encrypt PII", exception);
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            // Backwards-compatible read for rows awaiting the one-time backfill.
            return stored;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] nonce = Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to decrypt PII; verify PII_ENCRYPTION_KEY", exception);
        }
    }

    public static String searchHash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash PII", exception);
        }
    }

    public static boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static byte[] loadKey() {
        String configured = System.getenv("PII_ENCRYPTION_KEY");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("pii.encryption.key");
        }
        if (configured == null || configured.isBlank()) {
            String profiles = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE",
                    System.getProperty("spring.profiles.active", ""));
            if (Arrays.stream(profiles.split(","))
                    .map(String::trim)
                    .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                            || profile.equalsIgnoreCase("production"))) {
                throw new IllegalStateException("PII_ENCRYPTION_KEY is required for production profiles");
            }
            configured = "lorafilm-local-development-key-change-before-production";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configured);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Non-Base64 values are deliberately derived to a fixed AES-256 key.
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(configured.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize PII encryption", exception);
        }
    }
}
