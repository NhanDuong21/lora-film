package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.common.enums.ActionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration"})
@ActiveProfiles("test")
class CinemaClosurePeriodRepositoryIntegrationTest {

    @Autowired
    private CinemaClosurePeriodRepository closureRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    private Cinema cinema;

    @BeforeEach
    void setUp() {
        cinema = new Cinema();
        cinema.setName("Test Cinema");
        cinema.setSlug("test-cinema");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema.setAddress("123 Street");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinemaRepository.save(cinema);
    }

    private CinemaClosurePeriod createClosure(Instant start, Instant end) {
        CinemaClosurePeriod closure = new CinemaClosurePeriod();
        closure.setCinema(cinema);
        closure.setStartTime(start);
        closure.setEndTime(end);
        closure.setStatus(ActionStatus.ACTIVE);
        closure.setReason("Test closure");
        return closureRepository.save(closure);
    }

    @Test
    void findOverlappingClosures_shouldDetectOverlap_whenIntervalsIntersect() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createClosure(t10, t12);

        Instant t11 = Instant.parse("2026-07-15T11:00:00Z");
        Instant t13 = Instant.parse("2026-07-15T13:00:00Z");
        
        List<CinemaClosurePeriod> overlaps = closureRepository.findOverlappingClosures(cinema.getId(), t11, t13);
        assertFalse(overlaps.isEmpty(), "Should detect overlap");
    }

    @Test
    void findOverlappingClosures_shouldNotDetectOverlap_whenAdjacent() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createClosure(t10, t12);

        Instant t12_14 = Instant.parse("2026-07-15T14:00:00Z");
        
        List<CinemaClosurePeriod> overlaps = closureRepository.findOverlappingClosures(cinema.getId(), t12, t12_14);
        assertTrue(overlaps.isEmpty(), "Should NOT detect overlap for adjacent boundaries");
    }
}
