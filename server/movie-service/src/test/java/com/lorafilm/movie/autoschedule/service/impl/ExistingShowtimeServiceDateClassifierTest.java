package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExistingShowtimeServiceDateClassifierTest {

    private final ExistingShowtimeServiceDateClassifier classifier =
            new ExistingShowtimeServiceDateClassifier();
    private final ZoneId cinemaZone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LocalDate serviceDate = LocalDate.of(2026, 7, 23);
    private final OperatingWindow overnight = new OperatingWindow(
            serviceDate,
            Instant.parse("2026-07-23T13:00:00Z"),
            Instant.parse("2026-07-23T19:00:00Z"));

    @Test
    void overnightStartAfterLocalMidnightRetainsPreviousServiceDate() {
        assertEquals(serviceDate, classifier.classify(
                Instant.parse("2026-07-23T17:30:00Z"), cinemaZone, List.of(overnight)).orElseThrow());
    }

    @Test
    void openBoundaryIsIncludedAndCloseBoundaryIsExcluded() {
        assertEquals(serviceDate, classifier.classify(
                overnight.getOpenInstant(), cinemaZone, List.of(overnight)).orElseThrow());
        assertTrue(classifier.classify(
                overnight.getCloseInstant(), cinemaZone, List.of(overnight)).isEmpty());
    }

    @Test
    void instantOutsideEveryResolvedWindowIsUnclassifiable() {
        assertTrue(classifier.classify(
                Instant.parse("2026-07-23T12:59:59Z"), cinemaZone, List.of(overnight)).isEmpty());
    }
}
