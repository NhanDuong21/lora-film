package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class MaintenanceWindowIntegrationTest {



    @Autowired
    private AuditoriumMaintenanceWindowRepository maintenanceWindowRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    private Auditorium auditorium;

    @Autowired
    private com.lorafilm.movie.cinema.repository.CinemaRepository cinemaRepository;

    private com.lorafilm.movie.cinema.domain.entity.Cinema cinema;

    @BeforeEach
    void setUp() {
        maintenanceWindowRepository.deleteAll();
        auditoriumRepository.deleteAll();
        cinemaRepository.deleteAll();

        cinema = new com.lorafilm.movie.cinema.domain.entity.Cinema();
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinema.setSlug("cinema-" + System.currentTimeMillis());
        cinema.setName("Test Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema = cinemaRepository.saveAndFlush(cinema);

        auditorium = new Auditorium();
        auditorium.setPublicId("aud-maint-1");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setName("Maintenance Test Auditorium");
        auditorium.setCinema(cinema);
        auditorium.setCapacity(100);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        maintenanceWindowRepository.deleteAll();
        auditoriumRepository.deleteAll();
        cinemaRepository.deleteAll();
    }

    @Test
    void shouldDetectOverlapProperly() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant t1Start = now.plus(1, ChronoUnit.DAYS);
        Instant t1End = t1Start.plus(2, ChronoUnit.HOURS);

        AuditoriumMaintenanceWindow window1 = new AuditoriumMaintenanceWindow();
        window1.setAuditorium(auditorium);
        window1.setStartTime(t1Start);
        window1.setEndTime(t1End);
        maintenanceWindowRepository.saveAndFlush(window1);

        // Case 1: T1 = [8:00, 10:00), Request T2 = [9:00, 11:00) -> Overlap
        Instant t2Start = t1Start.plus(1, ChronoUnit.HOURS);
        Instant t2End = t1End.plus(1, ChronoUnit.HOURS);
        boolean isOverlap1 = maintenanceWindowRepository.existsOverlap(auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, t2Start, t2End);
        assertThat(isOverlap1).isTrue();

        // Case 2: T1 = [8:00, 10:00), Request T2 = [10:00, 12:00) -> NO Overlap (half-open interval)
        Instant t3Start = t1End;
        Instant t3End = t3Start.plus(2, ChronoUnit.HOURS);
        boolean isOverlap2 = maintenanceWindowRepository.existsOverlap(auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, t3Start, t3End);
        assertThat(isOverlap2).isFalse();
        
        // Case 3: T1 = [8:00, 10:00), Request T2 = [6:00, 8:00) -> NO Overlap
        Instant t4End = t1Start;
        Instant t4Start = t4End.minus(2, ChronoUnit.HOURS);
        boolean isOverlap3 = maintenanceWindowRepository.existsOverlap(auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, t4Start, t4End);
        assertThat(isOverlap3).isFalse();

        // Case 4: T1 = [8:00, 10:00), Request T2 = [7:00, 9:00) -> Overlap
        Instant t5Start = t1Start.minus(1, ChronoUnit.HOURS);
        Instant t5End = t1Start.plus(1, ChronoUnit.HOURS);
        boolean isOverlap4 = maintenanceWindowRepository.existsOverlap(auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, t5Start, t5End);
        assertThat(isOverlap4).isTrue();
    }
}
