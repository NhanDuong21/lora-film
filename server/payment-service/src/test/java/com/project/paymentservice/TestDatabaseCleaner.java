package com.project.paymentservice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.List;

@Component
class TestDatabaseCleaner {
    private static final List<String> TABLES = List.of(
            "accounting_audit_events",
            "settlement_entries",
            "settlement_batches",
            "accounting_periods",
            "payment_refunds",
            "payment_outbox_events",
            "payment_reconciliation_cases",
            "payment_webhook_events",
            "payment_idempotency_records",
            "payment_analytics_snapshots",
            "counter_cash_sessions",
            "cash_payment_details",
            "payment_logs",
            "booking_payment_guards",
            "payments");

    private final JdbcTemplate jdbcTemplate;

    TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void clean() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : TABLES) {
                    statement.execute("TRUNCATE TABLE " + table);
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (Exception exception) {
                try (Statement restore = connection.createStatement()) {
                    restore.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
                throw exception;
            }
            return null;
        });
    }
}
