package com.lorafilm.movie.auditorium.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration"})
@ActiveProfiles("test")
class AuditoriumMaintenanceWindowRepositoryIntegrationTest {

    @Autowired
    private AuditoriumMaintenanceWindowRepository maintenanceRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    private Auditorium auditorium;
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

        auditorium = new Auditorium();
        auditorium.setName("Screen 1");
        auditorium.setCinema(cinema);
        auditorium.setStatus(com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
        auditorium.setCapacity(100);
        auditorium.setPublicId(java.util.UUID.randomUUID().toString());
        auditorium.setScreenType(com.lorafilm.movie.auditorium.domain.enums.ScreenType.STANDARD);
        auditorium.setSoundType(com.lorafilm.movie.auditorium.domain.enums.SoundType.DOLBY_ATMOS);
        auditorium.setCleaningBufferMinutes(15);
        auditoriumRepository.save(auditorium);
    }

    private void createMaintenance(Instant start, Instant end) {
        AuditoriumMaintenanceWindow mw = new AuditoriumMaintenanceWindow();
        mw.setAuditorium(auditorium);
        mw.setStartTime(start);
        mw.setEndTime(end);
        mw.setStatus(ActionStatus.ACTIVE);
        mw.setReason("Test maintenance");
        maintenanceRepository.save(mw);
    }

    @Test
    void existsOverlap_shouldDetectOverlap_whenIntervalsIntersect() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createMaintenance(t10, t12);

        Instant t11 = Instant.parse("2026-07-15T11:00:00Z");
        Instant t13 = Instant.parse("2026-07-15T13:00:00Z");
        
        boolean overlap = maintenanceRepository.existsOverlap(auditorium.getId(), ActionStatus.ACTIVE, t11, t13);
        assertTrue(overlap, "Should detect overlap");
    }

    @Test
    void existsOverlap_shouldNotDetectOverlap_whenAdjacent() {
        Instant t10 = Instant.parse("2026-07-15T10:00:00Z");
        Instant t12 = Instant.parse("2026-07-15T12:00:00Z");
        createMaintenance(t10, t12);

        Instant t12_14 = Instant.parse("2026-07-15T14:00:00Z");
        
        boolean overlap = maintenanceRepository.existsOverlap(auditorium.getId(), ActionStatus.ACTIVE, t12, t12_14);
        assertFalse(overlap, "Should NOT detect overlap for adjacent boundaries");
    }
}
