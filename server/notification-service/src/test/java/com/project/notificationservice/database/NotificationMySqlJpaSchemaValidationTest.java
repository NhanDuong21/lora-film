package com.project.notificationservice.database;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@ContextConfiguration(initializers =
        NotificationMySqlJpaSchemaValidationTest.SchemaInitializer.class)
class NotificationMySqlJpaSchemaValidationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("notification_db")
                    .withUsername("notification")
                    .withPassword("notification-test-password");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private DataSource dataSource;

    @Test
    void canonicalSqlCharColumnsMatchEveryJpaEntity() throws Exception {
        assertThat(entityManagerFactory).isNotNull();
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND data_type = 'char'
                """;
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.createStatement().executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(8);
        }
    }

    static class SchemaInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Path schema = findCanonicalSchema();
            try (Connection connection = DriverManager.getConnection(
                    MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
                String schemaSql = Files.readString(schema, StandardCharsets.UTF_8);
                String useStatement = "USE notification_db;";
                int ddlStart = schemaSql.indexOf(useStatement);
                if (ddlStart < 0) {
                    throw new IllegalStateException(
                            "Canonical schema has no USE notification_db statement");
                }
                String tableDdl = schemaSql.substring(ddlStart + useStatement.length());
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(
                        tableDdl.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not apply canonical Notification schema: " + schema, exception);
            }
        }

        private static Path findCanonicalSchema() {
            Path workingDirectory = Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath()
                    .normalize();
            Path fromRepositoryRoot = workingDirectory.resolve(
                    "docs/database/mysql/schema/notification-service-schema.sql");
            if (Files.isRegularFile(fromRepositoryRoot)) return fromRepositoryRoot;

            Path fromServiceDirectory = workingDirectory.resolve(
                    "../../docs/database/mysql/schema/notification-service-schema.sql").normalize();
            if (Files.isRegularFile(fromServiceDirectory)) return fromServiceDirectory;

            throw new IllegalStateException(
                    "Không tìm thấy docs/database/mysql/schema/notification-service-schema.sql từ "
                            + workingDirectory);
        }
    }
}
