package com.lorafilm.movie.autoschedule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoScheduleRequestFingerprintServiceImplTest {

    private AutoScheduleRequestFingerprintServiceImpl fingerprintService;

    @BeforeEach
    void setUp() {
        fingerprintService = new AutoScheduleRequestFingerprintServiceImpl(new ObjectMapper());
    }

    @Test
    void generateFingerprint_producesConsistentHash() {
        NormalizedGeneratePreviewRequest req1 = new NormalizedGeneratePreviewRequest(
                "cinema-1",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 7),
                List.of("mv-1", "mv-2"),
                List.of("aud-1", "aud-2"),
                15,
                60,
                "key-1"
        );

        NormalizedGeneratePreviewRequest req2 = new NormalizedGeneratePreviewRequest(
                "cinema-1",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 7),
                List.of("mv-1", "mv-2"),
                List.of("aud-1", "aud-2"),
                15,
                60,
                "key-2" // Different idempotency key, should NOT affect fingerprint
        );

        String hash1 = fingerprintService.generateFingerprint(req1);
        String hash2 = fingerprintService.generateFingerprint(req2);

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 hex length
        assertEquals(hash1, hash2, "Fingerprints should be identical despite different idempotency keys");
    }

    @Test
    void generateFingerprint_differentInputsProduceDifferentHashes() {
        NormalizedGeneratePreviewRequest req1 = new NormalizedGeneratePreviewRequest(
                "cinema-1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 7),
                List.of("mv-1"), List.of("aud-1"), 15, 60, "key-1"
        );

        NormalizedGeneratePreviewRequest req2 = new NormalizedGeneratePreviewRequest(
                "cinema-1", LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 7), // Different date
                List.of("mv-1"), List.of("aud-1"), 15, 60, "key-1"
        );

        assertNotEquals(fingerprintService.generateFingerprint(req1), fingerprintService.generateFingerprint(req2));
    }
}
