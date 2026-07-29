package com.project.promotionservice.common.time;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DatabaseTimeProvider {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTimeProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Instant now() {
        Timestamp timestamp = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP(6)", Timestamp.class);
        if (timestamp == null) {
            throw new IllegalStateException("Database did not return its current timestamp");
        }
        return timestamp.toInstant().truncatedTo(ChronoUnit.MICROS);
    }
}
