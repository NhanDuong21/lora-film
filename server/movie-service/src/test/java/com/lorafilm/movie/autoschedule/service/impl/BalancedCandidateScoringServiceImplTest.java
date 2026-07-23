package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalancedCandidateScoringServiceImplTest {

    private BalancedCandidateScoringServiceImpl scoringService;
    private Cinema cinema;
    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        scoringService = new BalancedCandidateScoringServiceImpl();
        cinema = new Cinema();
        cinema.setId(1L);
        cinema.setTimezone("UTC"); // Use UTC for easy reasoning

        auditorium = new Auditorium();
        auditorium.setId(100L);
        auditorium.setCapacity(150);
    }

    @Test
    void score_primeTime_addsPrimeBonus() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setAuditorium(auditorium);
        candidate.setStartTime(ZonedDateTime.of(2023, 10, 1, 19, 0, 0, 0, ZoneId.of("UTC")).toInstant());

        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(), List.of());

        CandidateScoreResult result = scoringService.score(candidate, context);

        Map<String, BigDecimal> breakdown = result.getScoreBreakdown();
        // Base(50) + Prime(20) + Fit(10) = 80
        assertEquals(80.0, result.getScore().doubleValue(), 0.001);
        assertEquals(20.0, breakdown.get("primeTime").doubleValue(), 0.001);
        assertEquals(0.0, breakdown.get("offPeak").doubleValue(), 0.001);
    }

    @Test
    void score_offPeak_addsOffPeakBonus() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setAuditorium(auditorium);
        candidate.setStartTime(ZonedDateTime.of(2023, 10, 1, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant());

        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(), List.of());

        CandidateScoreResult result = scoringService.score(candidate, context);

        // Base(50) + OffPeak(5) + Fit(10) = 65
        assertEquals(65.0, result.getScore().doubleValue(), 0.001);
        assertEquals(5.0, result.getScoreBreakdown().get("offPeak").doubleValue(), 0.001);
    }

    @Test
    void score_earlySlot_addsEarlyBonus() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setAuditorium(auditorium);
        
        Instant windowOpen = ZonedDateTime.of(2023, 10, 1, 8, 0, 0, 0, ZoneId.of("UTC")).toInstant();
        Instant windowClose = ZonedDateTime.of(2023, 10, 1, 23, 0, 0, 0, ZoneId.of("UTC")).toInstant();
        OperatingWindow window = new OperatingWindow(windowOpen, windowClose);

        candidate.setStartTime(ZonedDateTime.of(2023, 10, 1, 8, 30, 0, 0, ZoneId.of("UTC")).toInstant());

        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(window), List.of());

        CandidateScoreResult result = scoringService.score(candidate, context);

        // Base(50) + Early(5) + Fit(10) = 65
        assertEquals(65.0, result.getScore().doubleValue(), 0.001);
        assertEquals(5.0, result.getScoreBreakdown().get("earlySlot").doubleValue(), 0.001);
    }

    @Test
    void score_scheduleContinuity_addsContinuityBonus() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setAuditorium(auditorium);
        // Start at 14:00
        candidate.setStartTime(ZonedDateTime.of(2023, 10, 1, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant());

        Showtime previous = new Showtime();
        auditorium.setCleaningBufferMinutes(15);
        previous.setAuditorium(auditorium);
        // Occupancy ends at 13:45 (EndTime 13:30 + 15m Buffer = 13:45). Gap to StartTime (14:00) is 15 minutes, <= 30
        previous.setEndTime(ZonedDateTime.of(2023, 10, 1, 13, 30, 0, 0, ZoneId.of("UTC")).toInstant());

        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(), List.of(previous));

        CandidateScoreResult result = scoringService.score(candidate, context);

        // Base(50) + Continuity(10) + Fit(10) = 70
        assertEquals(70.0, result.getScore().doubleValue(), 0.001);
        assertEquals(10.0, result.getScoreBreakdown().get("scheduleContinuity").doubleValue(), 0.001);
    }

    @Test
    void score_rejectedCandidate_returnsZero() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.REJECTED);
        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(), List.of());

        CandidateScoreResult result = scoringService.score(candidate, context);

        assertEquals(new BigDecimal("0.000"), result.getScore());
        assertEquals(Map.of(), result.getScoreBreakdown());
    }

    @Test
    void score_timeBoundariesAndPrimePrecedenceRemainExact() {
        CandidateScoringContext context = new CandidateScoringContext(cinema, List.of(), List.of());

        CandidateScoreResult atNine = scoringService.score(candidateAt(9, 0), context);
        CandidateScoreResult atTwelve = scoringService.score(candidateAt(12, 0), context);
        CandidateScoreResult atEighteen = scoringService.score(candidateAt(18, 0), context);
        CandidateScoreResult atTwentyTwo = scoringService.score(candidateAt(22, 0), context);

        assertEquals(new BigDecimal("5.000"), atNine.getScoreBreakdown().get("offPeak"));
        assertEquals(new BigDecimal("0.000"), atTwelve.getScoreBreakdown().get("offPeak"));
        assertEquals(new BigDecimal("20.000"), atEighteen.getScoreBreakdown().get("primeTime"));
        assertEquals(new BigDecimal("0.000"), atEighteen.getScoreBreakdown().get("offPeak"));
        assertEquals(new BigDecimal("0.000"), atTwentyTwo.getScoreBreakdown().get("primeTime"));
    }

    @Test
    void score_earlyAndContinuityBoundariesRemainExact() {
        Instant open = Instant.parse("2023-10-01T08:00:00Z");
        OperatingWindow window = new OperatingWindow(open, open.plusSeconds(8 * 3600));

        ShowtimeCandidate fiftyNineMinutes = candidateAt(8, 59);
        fiftyNineMinutes.setOperatingWindow(window);
        ShowtimeCandidate sixtyMinutes = candidateAt(9, 0);
        sixtyMinutes.setOperatingWindow(window);

        CandidateScoringContext windowContext = new CandidateScoringContext(
                cinema, List.of(window), List.of());
        assertEquals(new BigDecimal("5.000"), scoringService.score(
                fiftyNineMinutes, windowContext).getScoreBreakdown().get("earlySlot"));
        assertEquals(new BigDecimal("0.000"), scoringService.score(
                sixtyMinutes, windowContext).getScoreBreakdown().get("earlySlot"));

        ShowtimeCandidate candidate = candidateAt(14, 0);
        Showtime thirtyMinutePrevious = previousEndingAt("2023-10-01T13:30:00Z");
        Showtime thirtyOneMinutePrevious = previousEndingAt("2023-10-01T13:29:00Z");
        assertEquals(new BigDecimal("10.000"), scoringService.score(candidate,
                new CandidateScoringContext(cinema, List.of(), List.of(thirtyMinutePrevious)))
                .getScoreBreakdown().get("scheduleContinuity"));
        assertEquals(new BigDecimal("0.000"), scoringService.score(candidate,
                new CandidateScoringContext(cinema, List.of(), List.of(thirtyOneMinutePrevious)))
                .getScoreBreakdown().get("scheduleContinuity"));
    }

    @Test
    void score_validBreakdownHasStableComponentsScaleAndExactSum() {
        CandidateScoreResult result = scoringService.score(candidateAt(19, 0),
                new CandidateScoringContext(cinema, List.of(), List.of()));

        assertEquals(Set.of("base", "primeTime", "offPeak", "earlySlot",
                "auditoriumFit", "scheduleContinuity"), result.getScoreBreakdown().keySet());
        result.getScoreBreakdown().values().forEach(value -> assertEquals(3, value.scale()));
        BigDecimal sum = result.getScoreBreakdown().values().stream()
                .reduce(new BigDecimal("0.000"), BigDecimal::add);
        assertEquals(new BigDecimal("80.000"), result.getScore());
        assertEquals(result.getScore(), sum);
    }

    private ShowtimeCandidate candidateAt(int hour, int minute) {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setAuditorium(auditorium);
        candidate.setStartTime(ZonedDateTime.of(
                2023, 10, 1, hour, minute, 0, 0, ZoneId.of("UTC")).toInstant());
        return candidate;
    }

    private Showtime previousEndingAt(String end) {
        auditorium.setCleaningBufferMinutes(0);
        Showtime previous = new Showtime();
        previous.setAuditorium(auditorium);
        previous.setEndTime(Instant.parse(end));
        return previous;
    }
}
