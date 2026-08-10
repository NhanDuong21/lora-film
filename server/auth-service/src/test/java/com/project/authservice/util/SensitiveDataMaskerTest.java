package com.project.authservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataMaskerTest {

    @Test
    void masksEmailLocalPartAndNormalizesCase() {
        assertEquals("u***@example.com", SensitiveDataMasker.maskEmail(" User@Example.COM "));
    }

    @Test
    void handlesMissingAndMalformedValuesWithoutLeakingThem() {
        assertEquals("unknown", SensitiveDataMasker.maskEmail(null));
        assertEquals("unknown", SensitiveDataMasker.maskEmail(" "));
        assertEquals("n***", SensitiveDataMasker.maskEmail("not-an-email"));
    }
}
