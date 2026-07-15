package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BalancedCandidateScoringServiceImpl implements BalancedCandidateScoringService {

    @Override
    public CandidateScoreResult score(ShowtimeCandidate candidate, CandidateScoringContext context) {
        if (candidate.getValidationStatus() == PreviewItemValidationStatus.REJECTED) {
            return new CandidateScoreResult(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), new HashMap<>());
        }

        Map<String, BigDecimal> breakdown = new HashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        // Base Score
        BigDecimal baseScore = BigDecimal.valueOf(50);
        breakdown.put("base", baseScore);
        totalScore = totalScore.add(baseScore);

        ZoneId zoneId = ZoneId.of(context.getCinema().getTimezone());
        ZonedDateTime startZdt = candidate.getStartTime().atZone(zoneId);
        LocalTime startTime = startZdt.toLocalTime();

        // Prime Time Bonus (18:00 inclusive -> 22:00 exclusive)
        LocalTime primeStart = LocalTime.of(18, 0);
        LocalTime primeEnd = LocalTime.of(22, 0);
        if (!startTime.isBefore(primeStart) && startTime.isBefore(primeEnd)) {
            BigDecimal primeBonus = BigDecimal.valueOf(20);
            breakdown.put("primeTime", primeBonus);
            totalScore = totalScore.add(primeBonus);
            breakdown.put("offPeak", BigDecimal.ZERO);
        } else {
            breakdown.put("primeTime", BigDecimal.ZERO);
            
            // Off-Peak Bonus (09:00 inclusive -> 12:00 exclusive)
            LocalTime offPeakStart = LocalTime.of(9, 0);
            LocalTime offPeakEnd = LocalTime.of(12, 0);
            if (!startTime.isBefore(offPeakStart) && startTime.isBefore(offPeakEnd)) {
                BigDecimal offPeakBonus = BigDecimal.valueOf(5);
                breakdown.put("offPeak", offPeakBonus);
                totalScore = totalScore.add(offPeakBonus);
            } else {
                breakdown.put("offPeak", BigDecimal.ZERO);
            }
        }

        // Early Slot Bonus
        BigDecimal earlyBonus = BigDecimal.ZERO;
        Optional<OperatingWindow> matchedWindow = context.getOperatingWindows().stream()
                .filter(w -> !candidate.getStartTime().isBefore(w.getOpenInstant()) &&
                             !candidate.getStartTime().isAfter(w.getCloseInstant()))
                .findFirst();
                
        if (matchedWindow.isPresent()) {
            Instant openInstant = matchedWindow.get().getOpenInstant();
            if (ChronoUnit.MINUTES.between(openInstant, candidate.getStartTime()) < 60) {
                earlyBonus = BigDecimal.valueOf(5);
            }
        }
        breakdown.put("earlySlot", earlyBonus);
        totalScore = totalScore.add(earlyBonus);

        // Auditorium Fit Bonus
        BigDecimal fitBonus = BigDecimal.ZERO;
        if (candidate.getAuditorium() != null && candidate.getAuditorium().getCapacity() != null 
                && candidate.getAuditorium().getCapacity() > 0) {
            fitBonus = BigDecimal.valueOf(10);
        }
        breakdown.put("auditoriumFit", fitBonus);
        totalScore = totalScore.add(fitBonus);

        // Schedule Continuity Bonus
        BigDecimal continuityBonus = BigDecimal.ZERO;
        if (context.getExistingShowtimes() != null) {
            // Find existing showtimes in the same auditorium
            Showtime closestPrevious = context.getExistingShowtimes().stream()
                    .filter(st -> st.getAuditorium().getId().equals(candidate.getAuditorium().getId()))
                    .filter(st -> getOccupancyEnd(st).isBefore(candidate.getStartTime()) || getOccupancyEnd(st).equals(candidate.getStartTime()))
                    .max((s1, s2) -> getOccupancyEnd(s1).compareTo(getOccupancyEnd(s2)))
                    .orElse(null);

            if (closestPrevious != null) {
                long gapMinutes = ChronoUnit.MINUTES.between(getOccupancyEnd(closestPrevious), candidate.getStartTime());
                if (gapMinutes <= 30) {
                    continuityBonus = BigDecimal.valueOf(10);
                }
            }
        }
        breakdown.put("scheduleContinuity", continuityBonus);
        totalScore = totalScore.add(continuityBonus);

        // Standardize all decimals to scale 3
        totalScore = totalScore.setScale(3, RoundingMode.HALF_UP);
        for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
            breakdown.put(entry.getKey(), entry.getValue().setScale(3, RoundingMode.HALF_UP));
        }

        return new CandidateScoreResult(totalScore, breakdown);
    }

    private Instant getOccupancyEnd(Showtime showtime) {
        if (showtime.getAuditorium() != null && showtime.getAuditorium().getCleaningBufferMinutes() != null) {
            return showtime.getEndTime().plus(showtime.getAuditorium().getCleaningBufferMinutes(), ChronoUnit.MINUTES);
        }
        return showtime.getEndTime();
    }
}
