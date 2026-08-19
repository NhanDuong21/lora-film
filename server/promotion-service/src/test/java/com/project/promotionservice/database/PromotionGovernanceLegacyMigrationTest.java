package com.project.promotionservice.database;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PromotionGovernanceLegacyMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("promotion_db")
            .withUsername("promotion")
            .withPassword("promotion-test-password");

    @Test
    void migrationBackfillsLegacyReleaseAndPreservesEveryOtherLifecycleState()
            throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE promotion_reservations (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      status VARCHAR(30) NOT NULL,
                      rollback_at DATETIME(6) NULL,
                      rollback_reason VARCHAR(255) NULL,
                      updated_at DATETIME(6) NOT NULL,
                      updated_by VARCHAR(36) NULL
                    )
                    """);
            for (String status : List.of(
                    "ACTIVE", "RELEASED", "EXPIRED", "CONFIRMED", "REVERSED")) {
                statement.executeUpdate("INSERT INTO promotion_reservations "
                        + "(status, rollback_reason, updated_at, updated_by) VALUES ('"
                        + status + "', 'legacy detail', NOW(6), 'legacy-user')");
            }

            executeMigration(connection);
            executeMigration(connection);

            try (ResultSet released = statement.executeQuery("""
                    SELECT release_reason_type, source_service, released_by,
                           released_at, reason_detail
                      FROM promotion_reservations WHERE status = 'RELEASED'
                    """)) {
                assertThat(released.next()).isTrue();
                assertThat(released.getString("release_reason_type"))
                        .isEqualTo("LEGACY_UNKNOWN");
                assertThat(released.getString("source_service"))
                        .isEqualTo("LEGACY_MIGRATION");
                assertThat(released.getString("released_by")).isEqualTo("SYSTEM");
                assertThat(released.getTimestamp("released_at")).isNotNull();
                assertThat(released.getString("reason_detail"))
                        .isEqualTo("legacy detail");
            }
            try (ResultSet states = statement.executeQuery("""
                    SELECT status, release_reason_type
                      FROM promotion_reservations WHERE status <> 'RELEASED'
                      ORDER BY status
                    """)) {
                java.util.Map<String, String> actual = new java.util.LinkedHashMap<>();
                while (states.next()) {
                    actual.put(states.getString("status"),
                            states.getString("release_reason_type"));
                }
                assertThat(actual.keySet()).containsExactly(
                        "ACTIVE", "CONFIRMED", "EXPIRED", "REVERSED");
                assertThat(actual.values()).containsOnlyNulls();
            }
        }
    }

    private void executeMigration(Connection connection) throws Exception {
        String script = Files.readString(findMigration(), StandardCharsets.UTF_8)
                .replace("DELIMITER $$", "")
                .replace("DELIMITER ;", "");
        try (Statement statement = connection.createStatement()) {
            for (String command : script.split("\\$\\$")) {
                if (!command.isBlank()) statement.execute(command.trim());
            }
        }
    }

    private Path findMigration() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        Path fromRoot = workingDirectory.resolve(
                "docs/database/mysql/migrations/20260819_promotion_governance_refine.sql");
        if (Files.isRegularFile(fromRoot)) return fromRoot;
        Path fromService = workingDirectory.resolve(
                "../../docs/database/mysql/migrations/20260819_promotion_governance_refine.sql")
                .normalize();
        if (Files.isRegularFile(fromService)) return fromService;
        throw new IllegalStateException("Cannot locate promotion governance migration");
    }
}
