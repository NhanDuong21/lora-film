package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CandidateSelectionResolverImpl implements CandidateSelectionResolver {

    @Override
    public void resolveDefaultSelection(List<ShowtimeCandidate> candidates) {
        // 1. Sort all candidates to determine deterministic ranking
        candidates.sort(
                Comparator.comparing((ShowtimeCandidate c) -> c.getValidationStatus() == PreviewItemValidationStatus.VALID ? 0 : 1)
                        .thenComparing(c -> c.getScore() != null ? c.getScore() : java.math.BigDecimal.ZERO, Comparator.reverseOrder())
                        .thenComparing(ShowtimeCandidate::getStartTime)
                        .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
                        .thenComparing(ShowtimeCandidate::getMovieVersionPublicId)
                        .thenComparing(c -> c.getRejectionCode() != null ? c.getRejectionCode() : "")
        );

        // Assign ranking positions
        int rank = 1;
        for (ShowtimeCandidate candidate : candidates) {
            candidate.setRankingPosition(rank++);
            candidate.setSelected(false); // Default false for all initially
        }

        // 2. Greedy Overlap Resolver
        List<ShowtimeCandidate> selectedCandidates = new ArrayList<>();

        for (ShowtimeCandidate candidate : candidates) {
            if (candidate.getValidationStatus() != PreviewItemValidationStatus.VALID) {
                continue;
            }
            boolean hasOverlap = false;

            // Check overlap against already selected candidates
            for (ShowtimeCandidate selected : selectedCandidates) {
                if (selected.getAuditoriumId().equals(candidate.getAuditoriumId())) {
                    // Check occupancy overlap: A.start < B.occupancyEnd AND A.occupancyEnd > B.start
                    if (candidate.getStartTime().isBefore(selected.getOccupancyEndTime()) &&
                            candidate.getOccupancyEndTime().isAfter(selected.getStartTime())) {
                        hasOverlap = true;
                        break;
                    }
                }
            }

            if (!hasOverlap) {
                candidate.setSelected(true);
                selectedCandidates.add(candidate);
            }
        }
    }
}
