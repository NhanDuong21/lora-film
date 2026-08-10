package com.project.userservice.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiCryptoTest {
    @Test
    void encryptsWithRandomNonceAndKeepsDeterministicSearchToken() {
        String first = PiiCrypto.encrypt("0909000001");
        String second = PiiCrypto.encrypt("0909000001");

        assertThat(first).startsWith("enc:v1:").isNotEqualTo(second);
        assertThat(PiiCrypto.decrypt(first)).isEqualTo("0909000001");
        assertThat(PiiCrypto.searchHash("0909 000 001"))
                .isEqualTo(PiiCrypto.searchHash("0909000001"));
    }

    @Test
    void readsLegacyPlaintextDuringBackfillWindow() {
        assertThat(PiiCrypto.decrypt("012345678901")).isEqualTo("012345678901");
    }
}
