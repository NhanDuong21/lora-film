package com.lorafilm.movie.autoschedule.repository;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ShowtimeSchedulePreviewItemServiceDateMigrationIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("movie_service_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void oneTimeMigrationPreservesLegacyRowsAndAddsNoIndex() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE showtime_schedule_preview_items (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        preview_id BIGINT NOT NULL,
                        ranking_position INT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO showtime_schedule_preview_items (preview_id, ranking_position)
                    VALUES (11, 1), (11, 2)
                    """);

            assertThat(columnCount(statement)).isZero();
            assertThat(rowCount(statement)).isEqualTo(2);

            String migrationSql = Files.readString(findMigration())
                    .replaceFirst(";\\s*$", "");
            statement.execute(migrationSql);

            assertThat(columnCount(statement)).isOne();
            assertThat(rowCount(statement)).isEqualTo(2);
            assertThat(nullServiceDateCount(statement)).isEqualTo(2);
            assertThat(serviceDateIndexCount(statement)).isZero();

            statement.executeUpdate("""
                    INSERT INTO showtime_schedule_preview_items
                        (preview_id, ranking_position, service_date)
                    VALUES (12, 1, '2026-07-24')
                    """);
            try (ResultSet result = statement.executeQuery("""
                    SELECT service_date
                    FROM showtime_schedule_preview_items
                    WHERE preview_id = 12
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getDate(1).toLocalDate().toString()).isEqualTo("2026-07-24");
            }
        }
    }

    private int columnCount(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'showtime_schedule_preview_items'
                  AND column_name = 'service_date'
                """)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int rowCount(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT COUNT(*) FROM showtime_schedule_preview_items")) {
            result.next();
            return result.getInt(1);
        }
    }

    private int nullServiceDateCount(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM showtime_schedule_preview_items
                WHERE service_date IS NULL
                """)) {
            result.next();
            return result.getInt(1);
        }
    }

    private int serviceDateIndexCount(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'showtime_schedule_preview_items'
                  AND column_name = 'service_date'
                """)) {
            result.next();
            return result.getInt(1);
        }
    }

    private Path findMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "docs", "database", "mysql", "migrations",
                    "20260722_add_showtime_schedule_preview_item_service_date.sql"));
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the service-date migration SQL");
    }
}
