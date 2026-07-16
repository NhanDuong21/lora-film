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
@SuppressWarnings("null")
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
        // Do not call cinemaRepository.deleteAll() to avoid foreign key violations with seeded data.
        closurePeriodRepository.deleteAll();

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
        if (cinema != null && cinema.getId() != null) {
            try {
                cinemaRepository.delete(cinema);
                cinemaRepository.flush();
            } catch (Exception e) {
                // Ignore if it fails due to constraint
            }
        }
    }

    @Test
    void testCinemaStatusTransitionsAndCustomerVisibility() {
        Instant now = Instant.now();

        // 1. Verify future closure period does NOT trigger a transition
        CinemaClosurePeriod futureClosure = new CinemaClosurePeriod();
        futureClosure.setCinema(cinema);
        futureClosure.setStartTime(now.plus(1, ChronoUnit.HOURS)); // Starts in 1 hour
        futureClosure.setEndTime(now.plus(2, ChronoUnit.HOURS));
        futureClosure.setReason("Scheduled maintenance");
        futureClosure.setStatus(ActionStatus.ACTIVE);
        closurePeriodRepository.saveAndFlush(futureClosure);

        // Run scheduler
        cinemaStatusScheduler.checkAndTransitionCinemaStatuses();

        // Cinema must remain ACTIVE
        Cinema cinemaAfterFutureSetup = cinemaRepository.findById(cinema.getId()).orElseThrow();
        assertThat(cinemaAfterFutureSetup.getStatus()).isEqualTo(CinemaStatus.ACTIVE);

        // 2. Initially, cinema is ACTIVE. Retrieve via customer service list and detail.
        PageResponse<CinemaDto> listResult = cinemaService.getCinemas("HCM", null, null, 0, 10);
        CinemaDto foundActive = listResult.getData().stream()
                .filter(c -> c.getPublicId().equals(cinema.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertThat(foundActive.getStatus()).isEqualTo("ACTIVE");

        CinemaDetailDto detailResult = cinemaService.getCinemaByIdentifier(cinema.getPublicId());
        assertThat(detailResult).isNotNull();
        assertThat(detailResult.getStatus()).isEqualTo("ACTIVE");

        // 3. Add an active closure period that covers "now"
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

        // 4. Verify customer visibility during TEMPORARILY_CLOSED status
        // A. List API should still return it
        PageResponse<CinemaDto> listResultClosed = cinemaService.getCinemas("HCM", null, null, 0, 10);
        CinemaDto foundClosed = listResultClosed.getData().stream()
                .filter(c -> c.getPublicId().equals(cinema.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertThat(foundClosed.getStatus()).isEqualTo("TEMPORARILY_CLOSED");

        // B. Detail API should still return it
        CinemaDetailDto detailResultClosed = cinemaService.getCinemaByIdentifier(cinema.getPublicId());
        assertThat(detailResultClosed).isNotNull();
        assertThat(detailResultClosed.getStatus()).isEqualTo("TEMPORARILY_CLOSED");

        // C. Closure periods API should still allow retrieval
        List<CinemaClosurePeriodResponse> closures = cinemaService.getCinemaClosurePeriods(cinema.getPublicId());
        assertThat(closures).isNotEmpty();
        boolean hasActiveRepairClosure = closures.stream().anyMatch(c -> "Unexpected repair".equals(c.getReason()));
        assertThat(hasActiveRepairClosure).isTrue();

        // 5. Deactivate or move activeClosure to the past so there are no active closures anymore
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
        CinemaDto foundRestored = listResultRestored.getData().stream()
                .filter(c -> c.getPublicId().equals(cinema.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertThat(foundRestored.getStatus()).isEqualTo("ACTIVE");
    }
}
