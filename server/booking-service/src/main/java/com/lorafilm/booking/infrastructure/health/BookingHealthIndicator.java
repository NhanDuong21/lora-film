package com.lorafilm.booking.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class BookingHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public BookingHealthIndicator(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.redisConnectionFactory = redisConnectionFactory;
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

        // 2. Redis Health Check
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pingResult = connection.ping();
            if ("PONG".equalsIgnoreCase(pingResult)) {
                details.put("redis", "UP");
            } else {
                details.put("redis", "DOWN (Unexpected response: " + pingResult + ")");
                overallUp = false;
            }
        } catch (Exception e) {
            details.put("redis", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        // 3. Outbox Table Accessibility
        try {
            jdbcTemplate.queryForObject("SELECT count(*) FROM booking_outbox_events", Integer.class);
            details.put("outboxTable", "UP");
        } catch (Exception e) {
            details.put("outboxTable", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        // 4. Inbox Table Accessibility
        try {
            jdbcTemplate.queryForObject("SELECT count(*) FROM booking_inbox_events", Integer.class);
            details.put("inboxTable", "UP");
        } catch (Exception e) {
            details.put("inboxTable", "DOWN (" + e.getMessage() + ")");
            overallUp = false;
        }

        if (overallUp) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
