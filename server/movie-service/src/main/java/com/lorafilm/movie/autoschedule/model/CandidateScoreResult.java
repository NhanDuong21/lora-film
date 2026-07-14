package com.lorafilm.movie.autoschedule.model;

import java.math.BigDecimal;
import java.util.Map;

public class CandidateScoreResult {
    private final BigDecimal score;
    private final Map<String, BigDecimal> scoreBreakdown;

    public CandidateScoreResult(BigDecimal score, Map<String, BigDecimal> scoreBreakdown) {
        this.score = score;
        this.scoreBreakdown = scoreBreakdown;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Map<String, BigDecimal> getScoreBreakdown() {
        return scoreBreakdown;
    }
}
