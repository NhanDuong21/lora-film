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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class MaintenanceWindowIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.32");

    @Autowired
    private AuditoriumMaintenanceWindowRepository maintenanceWindowRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        maintenanceWindowRepository.deleteAll();
        auditoriumRepository.deleteAll();

        auditorium = new Auditorium();
        auditorium.setPublicId("aud-maint-1");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setName("Maintenance Test Auditorium");
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
    }

    @Test
    void shouldDetectOverlapProperly() {
        Instant now = Instant.now();
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
