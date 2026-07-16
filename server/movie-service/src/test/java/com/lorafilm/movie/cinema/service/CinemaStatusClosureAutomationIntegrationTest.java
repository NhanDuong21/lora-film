package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.scheduler.CinemaStatusScheduler;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class CinemaStatusClosureAutomationIntegrationTest {

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private CinemaClosurePeriodRepository closurePeriodRepository;

    @Autowired
    private CinemaStatusScheduler cinemaStatusScheduler;

    @Autowired
    private CinemaService cinemaService;

    private Cinema cinema;

    @BeforeEach
    void setUp() {
        closurePeriodRepository.deleteAll();
        cinemaRepository.deleteAll();

        cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setSlug("test-cinema-" + System.currentTimeMillis());
        cinema.setName("Automation Test Cinema");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setAddress("123 Test St");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.saveAndFlush(cinema);
    }

    @AfterEach
    void tearDown() {
        closurePeriodRepository.deleteAll();
        cinemaRepository.deleteAll();
    }

    @Test
    void testCinemaStatusTransitionsAndCustomerVisibility() {
        Instant now = Instant.now();

        // 1. Initially, cinema is ACTIVE. Retrieve via customer service list and detail.
        PageResponse<CinemaDto> listResult = cinemaService.getCinemas("HCM", null, null, 0, 10);
        assertThat(listResult.getData()).isNotEmpty();
        assertThat(listResult.getData().get(0).getPublicId()).isEqualTo(cinema.getPublicId());
        assertThat(listResult.getData().get(0).getStatus()).isEqualTo("ACTIVE");

        CinemaDetailDto detailResult = cinemaService.getCinemaByIdentifier(cinema.getPublicId());
        assertThat(detailResult).isNotNull();
        assertThat(detailResult.getStatus()).isEqualTo("ACTIVE");

        // 2. Add an active closure period that covers "now"
        CinemaClosurePeriod activeClosure = new CinemaClosurePeriod();
        activeClosure.setCinema(cinema);
        activeClosure.setStartTime(now.minus(5, ChronoUnit.MINUTES));
        activeClosure.setEndTime(now.plus(5, ChronoUnit.MINUTES));
        activeClosure.setReason("Unexpected repair");
        activeClosure.setStatus(ActionStatus.ACTIVE);
        closurePeriodRepository.saveAndFlush(activeClosure);

        // Run scheduler
        cinemaStatusScheduler.checkAndTransitionCinemaStatuses();

        // Verify status transitioned to TEMPORARILY_CLOSED
        Cinema updatedCinema = cinemaRepository.findById(cinema.getId()).orElseThrow();
        assertThat(updatedCinema.getStatus()).isEqualTo(CinemaStatus.TEMPORARILY_CLOSED);

        // 3. Verify customer visibility during TEMPORARILY_CLOSED status
        // A. List API should still return it
        PageResponse<CinemaDto> listResultClosed = cinemaService.getCinemas("HCM", null, null, 0, 10);
        assertThat(listResultClosed.getData()).isNotEmpty();
        assertThat(listResultClosed.getData().get(0).getStatus()).isEqualTo("TEMPORARILY_CLOSED");

        // B. Detail API should still return it
        CinemaDetailDto detailResultClosed = cinemaService.getCinemaByIdentifier(cinema.getPublicId());
        assertThat(detailResultClosed).isNotNull();
        assertThat(detailResultClosed.getStatus()).isEqualTo("TEMPORARILY_CLOSED");

        // C. Closure periods API should still allow retrieval
        List<CinemaClosurePeriodResponse> closures = cinemaService.getCinemaClosurePeriods(cinema.getPublicId());
        assertThat(closures).isNotEmpty();
        assertThat(closures.get(0).getReason()).isEqualTo("Unexpected repair");

        // 4. Change closure period to have ended (in the past)
        activeClosure.setStartTime(now.minus(10, ChronoUnit.MINUTES));
        activeClosure.setEndTime(now.minus(2, ChronoUnit.MINUTES));
        closurePeriodRepository.saveAndFlush(activeClosure);

        // Run scheduler again
        cinemaStatusScheduler.checkAndTransitionCinemaStatuses();

        // Verify status transitioned back to ACTIVE
        Cinema restoredCinema = cinemaRepository.findById(cinema.getId()).orElseThrow();
        assertThat(restoredCinema.getStatus()).isEqualTo(CinemaStatus.ACTIVE);

        // List API should return it with ACTIVE status
        PageResponse<CinemaDto> listResultRestored = cinemaService.getCinemas("HCM", null, null, 0, 10);
        assertThat(listResultRestored.getData()).isNotEmpty();
        assertThat(listResultRestored.getData().get(0).getStatus()).isEqualTo("ACTIVE");
    }
}
