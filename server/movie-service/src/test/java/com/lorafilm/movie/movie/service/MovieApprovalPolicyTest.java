package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MovieApprovalPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);

    @Mock private ShowtimeRepository showtimeRepository;

    private MovieApprovalPolicy approvalPolicy;

    @BeforeEach
    void setUp() {
        MovieLifecyclePolicy lifecyclePolicy = new MovieLifecyclePolicy(
                Clock.fixed(Instant.parse("2026-08-04T02:00:00Z"), ZoneOffset.UTC));
        approvalPolicy = new MovieApprovalPolicy(lifecyclePolicy, showtimeRepository);
    }

    @Test
    void futureDraftTargetsUpcomingWithoutRequiringAShowtime() {
        Movie movie = movie(TODAY.plusDays(1));

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.UPCOMING, decision.targetStatus());
        assertTrue(decision.blockers().isEmpty());
        verifyNoInteractions(showtimeRepository);
    }

    @Test
    void draftWithTodaysReleaseDateCannotBeApproved() {
        Movie movie = movie(TODAY);

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.UPCOMING, decision.targetStatus());
        assertTrue(decision.blockers().stream().anyMatch(message -> message.contains("sau hôm nay")));
        verifyNoInteractions(showtimeRepository);
    }

    @Test
    void oldDraftRequiresANewFutureExploitationDate() {
        Movie movie = movie(TODAY.minusDays(10));

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.UPCOMING, decision.targetStatus());
        assertTrue(decision.blockers().stream().anyMatch(message -> message.contains("sau hôm nay")));
        verifyNoInteractions(showtimeRepository);
    }

    private Movie movie(LocalDate releaseDate) {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setStatus(MovieStatus.DRAFT);
        movie.setReleaseDate(releaseDate);
        return movie;
    }
}
