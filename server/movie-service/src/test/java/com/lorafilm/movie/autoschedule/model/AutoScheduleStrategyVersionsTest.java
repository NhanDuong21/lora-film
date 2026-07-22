package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoScheduleStrategyVersionsTest {

    @Test
    void s4IsSupportedButS3RemainsCurrentUntilActivationCheckpoint() {
        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3,
                AutoScheduleStrategyVersions.CURRENT);
        assertTrue(AutoScheduleStrategyVersions.isSupported(
                AutoScheduleStrategyVersions.BALANCED_V1_S4));
        assertTrue(AutoScheduleStrategyVersions.SUPPORTED.contains(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1));
        assertTrue(AutoScheduleStrategyVersions.SUPPORTED.contains(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2));
        assertTrue(AutoScheduleStrategyVersions.SUPPORTED.contains(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3));
        assertFalse(AutoScheduleStrategyVersions.isSupported("BALANCED_UNKNOWN"));
    }

    @Test
    void previewFactoryRetainsAnExplicitVersionWithoutChangingCurrent() {
        Cinema cinema = new Cinema();
        cinema.setTimezone("UTC");
        LocalDate date = LocalDate.of(2026, 7, 23);

        ShowtimeSchedulePreview preview = ShowtimeSchedulePreview.createGenerating(
                cinema, date, date, 15, 60,
                AutoScheduleStrategyVersions.BALANCED_V1_S4,
                "key", "fingerprint", 1L, Instant.parse("2026-07-22T17:00:00Z"));

        assertEquals(AutoScheduleStrategyVersions.BALANCED_V1_S4,
                preview.getStrategyVersion());
        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3,
                AutoScheduleStrategyVersions.CURRENT);
    }
}
