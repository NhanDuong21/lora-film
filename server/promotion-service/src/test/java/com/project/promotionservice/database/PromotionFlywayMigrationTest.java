package com.project.promotionservice.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PromotionFlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("promotion_service")
                    .withUsername("promotion")
                    .withPassword("promotion-test-password");

    @Test
    void migrationsBuildTheProductionRuntimeSchema() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
        assertColumnExists("promotion_reservations", "reservation_scope_key");
        assertColumnExists("promotion_reservations", "expiration_next_attempt_at");
        assertColumnExists("outbox_events", "processing_started_at");
        assertIndexExists("promotion_reservations", "uk_promotion_reservation_scope");
        assertIndexExists("promotion_idempotency_keys", "uk_idempotency_scope");
        assertIndexExists("outbox_events", "idx_outbox_claim");
        assertColumnExists("partners", "version");
        assertColumnExists("promotion_configurations", "requires_restart");
        assertColumnExists("promotion_integration_events", "schema_version");
        assertIndexExists("promotion_scheduler_locks", "PRIMARY");
        assertColumnType("promotion_reservations", "user_public_id", "varchar");
        assertColumnType("promotion_idempotency_keys", "request_hash", "varchar");
    }

    private void assertColumnExists(String tableName, String columnName) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;
        assertThat(queryCount(sql, tableName, columnName)).isEqualTo(1);
    }

    private void assertIndexExists(String tableName, String indexName) throws Exception {
        String sql = """
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """;
        assertThat(queryCount(sql, tableName, indexName)).isEqualTo(1);
    }

    private void assertColumnType(String tableName, String columnName, String expectedType)
            throws Exception {
        String sql = """
                SELECT DATA_TYPE
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (Connection connection = MYSQL.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo(expectedType);
            }
        }
    }

    private long queryCount(String sql, String first, String second) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, first);
            statement.setString(2, second);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
        }
    }
}
