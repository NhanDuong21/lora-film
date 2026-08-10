package com.project.paymentservice.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ProviderCrypto {
    private ProviderCrypto() {
    }

    public static String hmacHex(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot calculate provider signature", exception);
        }
    }

    public static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                actual.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }
}
