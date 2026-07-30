package com.project.notificationservice.service;

import com.project.notificationservice.exception.NotificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class RecipientCryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public RecipientCryptoService(
            @Value("${notification.security.recipient-encryption-key:}") String encodedKey) {
        this.key = decodeKey(encodedKey);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return null;
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new NotificationException("RECIPIENT_ENCRYPTION_FAILED",
                    "Recipient data could not be encrypted", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        requireKey();
        try {
            byte[] input = Base64.getDecoder().decode(encrypted);
            byte[] iv = Arrays.copyOfRange(input, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(input, IV_LENGTH, input.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new NotificationException("RECIPIENT_DECRYPTION_FAILED",
                    "Recipient data could not be decrypted", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new NotificationException("RECIPIENT_KEY_NOT_CONFIGURED",
                    "NOTIFICATION_RECIPIENT_ENCRYPTION_KEY is required",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private SecretKey decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedKey);
            if (bytes.length != 32) throw new IllegalArgumentException("key length");
            return new SecretKeySpec(bytes, "AES");
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "NOTIFICATION_RECIPIENT_ENCRYPTION_KEY must be a Base64-encoded 32-byte key");
        }
    }
}
