package com.project.promotionservice.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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
        Flyway flywayToV9 = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .target("9")
                .load();

        assertThat(flywayToV9.migrate().migrationsExecuted).isEqualTo(9);
        seedLegacyVoucherClones();

        Flyway flywayToV10 = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("10")
                .load();

        assertThat(flywayToV10.migrate().migrationsExecuted).isEqualTo(1);
        assertPromotionState("clone-public-1", "source-public", true);
        assertPromotionState("clone-public-2", "source-public", true);
        assertPromotionState("clone-private", "source-private", false);

        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertColumnDoesNotExist("promotion_campaigns", "campaign_type");
        assertColumnExists("promotions", "promotion_type");
        assertColumnExists("promotions", "cloned_from_public_id");
        assertColumnExists("user_promotions", "promotion_public_id");
        assertColumnExists("promotion_redemptions", "user_promotion_public_id");
        assertColumnExists("promotion_redemptions", "campaign_public_id");
        assertColumnExists("promotion_redemptions", "conditions_snapshot_json");
        assertColumnExists("promotion_redemptions", "actions_snapshot_json");
        assertColumnExists("promotion_redemptions", "sequence_no");
        assertColumnDoesNotExist("promotion_reservations", "reservation_type");
        assertColumnDoesNotExist("promotion_reservations", "coupon_public_id");
        assertColumnDoesNotExist("promotion_reservations", "voucher_public_id");
        assertIndexExists("promotions", "idx_promotion_discovery");
        assertIndexExists("promotions", "idx_promotions_cloned_from");
        assertIndexExists("user_promotions", "idx_user_promotion_owner_template");
        assertIndexExists("promotion_redemptions", "idx_promotion_redemption_campaign");
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

    private void seedLegacyVoucherClones() throws Exception {
        String sql = """
                INSERT INTO promotions (
                    public_id, campaign_public_id, promotion_type, code, name,
                    status, is_public, conditions_json, actions_json,
                    valid_from, valid_to
                ) VALUES
                    ('source-public', 'campaign-1', 'VOUCHER', 'EVENT',
                     'Event voucher', 'ACTIVE', TRUE, JSON_OBJECT(), JSON_OBJECT(),
                     '2026-01-01 00:00:00', '2027-01-01 00:00:00'),
                    ('clone-public-1', 'campaign-1', 'VOUCHER', 'EVENT_COPY_ABC123',
                     'Event voucher (Copy)', 'ACTIVE', FALSE, JSON_OBJECT(), JSON_OBJECT(),
                     '2026-01-01 00:00:00', '2027-01-01 00:00:00'),
                    ('clone-public-2', 'campaign-1', 'VOUCHER', 'EVENT_COPY_DEF456',
                     'Event voucher (Copy)', 'ACTIVE', FALSE, JSON_OBJECT(), JSON_OBJECT(),
                     '2026-01-01 00:00:00', '2027-01-01 00:00:00'),
                    ('source-private', 'campaign-1', 'VOUCHER', 'PRIVATE',
                     'Private voucher', 'ACTIVE', FALSE, JSON_OBJECT(), JSON_OBJECT(),
                     '2026-01-01 00:00:00', '2027-01-01 00:00:00'),
                    ('clone-private', 'campaign-1', 'VOUCHER', 'PRIVATE_COPY_GHI789',
                     'Private voucher (Copy)', 'ACTIVE', FALSE, JSON_OBJECT(), JSON_OBJECT(),
                     '2026-01-01 00:00:00', '2027-01-01 00:00:00')
                """;
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void assertPromotionState(
            String publicId, String clonedFromPublicId, boolean publicVisible)
            throws Exception {
        String sql = """
                SELECT cloned_from_public_id, is_public
                FROM promotions
                WHERE public_id = ?
                """;
        try (Connection connection = MYSQL.createConnection("");
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, publicId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("cloned_from_public_id"))
                        .isEqualTo(clonedFromPublicId);
                assertThat(resultSet.getBoolean("is_public")).isEqualTo(publicVisible);
            }
        }
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
