package com.project.scoreservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScoreServiceHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public ScoreServiceHealthIndicator(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean overallUp = true;

        // 1. MySQL Health Check
        try {
            jdbcTemplate.execute("SELECT 1");
            details.put("database", "UP");
        } catch (Exception e) {
            details.put("database", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        // 2. Outbox Table Check
        try {
            Integer pendingCount = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events WHERE status = 'PENDING'", Integer.class);
            details.put("outboxPendingEvents", pendingCount != null ? pendingCount : 0);
            details.put("outboxTable", "UP");
        } catch (Exception e) {
            details.put("outboxTable", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        // 3. User Score Table Check
        try {
            Integer userScoreCount = jdbcTemplate.queryForObject("SELECT count(*) FROM user_scores", Integer.class);
            details.put("totalUsersWithScores", userScoreCount != null ? userScoreCount : 0);
            details.put("userScoresTable", "UP");
        } catch (Exception e) {
            details.put("userScoresTable", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        if (overallUp) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
