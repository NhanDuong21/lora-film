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

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(9);
        assertColumnDoesNotExist("promotion_campaigns", "campaign_type");
        assertColumnExists("promotions", "promotion_type");
        assertColumnExists("promotions", "cloned_from_public_id");
        assertColumnExists("user_promotions", "promotion_public_id");
        assertColumnExists("promotion_redemptions", "user_promotion_public_id");
        assertColumnDoesNotExist("promotion_reservations", "reservation_type");
        assertColumnDoesNotExist("promotion_reservations", "coupon_public_id");
        assertColumnDoesNotExist("promotion_reservations", "voucher_public_id");
        assertIndexExists("promotions", "idx_promotion_discovery");
        assertIndexExists("promotions", "idx_promotions_cloned_from");
        assertIndexExists("user_promotions", "uk_user_promotion_owner_template");
        assertIndexExists("promotion_reservations", "idx_reservation_history_v2");
        assertColumnExists("promotion_reservations", "reservation_scope_key");
        assertColumnExists("promotion_reservations", "expiration_next_attempt_at");
        assertColumnExists("outbox_events", "processing_started_at");
        assertIndexExists("promotion_reservations", "uk_promotion_reservation_scope");
        assertIndexExists("promotion_idempotency_keys", "uk_idempotency_scope");
        assertIndexExists("outbox_events", "idx_outbox_claim");
        assertTableDoesNotExist("partners");
        assertTableDoesNotExist("partner_settlements");
        assertTableDoesNotExist("compensation_vouchers");
        assertTableDoesNotExist("coupons");
        assertTableDoesNotExist("vouchers");
        assertTableDoesNotExist("coupon_redemptions");
        assertTableDoesNotExist("voucher_redemptions");
        assertTableDoesNotExist("promotion_rules");
        assertColumnDoesNotExist("promotion_campaigns", "funding_source");
        assertColumnDoesNotExist("promotion_campaigns", "partner_public_id");
        assertColumnDoesNotExist("coupons", "partner_public_id");
        assertColumnDoesNotExist("vouchers", "partner_public_id");
        assertColumnExists("promotion_configurations", "requires_restart");
        assertColumnExists("promotion_integration_events", "schema_version");
        assertIndexExists("promotion_scheduler_locks", "PRIMARY");
        assertColumnType("promotion_reservations", "user_public_id", "varchar");
        assertColumnType("promotion_idempotency_keys", "request_hash", "varchar");
        assertColumnType("promotions", "public_id", "varchar");
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

    private void assertColumnDoesNotExist(String tableName, String columnName) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;
        assertThat(queryCount(sql, tableName, columnName)).isZero();
    }

    private void assertTableDoesNotExist(String tableName) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """;
        assertThat(queryCount(sql, tableName)).isZero();
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

    private long queryCount(String sql, String value) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
        }
    }
}
